/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.feature;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.postgis.jdbc.PGbox2d;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBWriter;
import org.postgresql.util.PGobject;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.datastore.feature.FeatureFilterBase;
import org.sensorhub.api.datastore.feature.FeatureKey;
import org.sensorhub.api.datastore.feature.IFeatureStoreBase;
import org.sensorhub.impl.datastore.DataStoreUtils;
import org.sensorhub.impl.datastore.postgis.IdProviderType;
import org.sensorhub.impl.datastore.postgis.PostgisStore;
import org.sensorhub.impl.datastore.postgis.builder.QueryBuilderBaseFeatureStore;
import org.sensorhub.impl.datastore.postgis.builder.QueryBuilderFeatureStore;
import org.sensorhub.impl.datastore.postgis.utils.PostgisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.ogc.gml.IFeature;
import org.vast.sensorML.SMLJsonBindings;
import org.vast.util.Bbox;

import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.sensorhub.api.datastore.feature.IFeatureStoreBase.FeatureField.VALID_TIME;
import static org.sensorhub.impl.datastore.postgis.utils.PostgisUtils.MIN_INSTANT;

public abstract class PostgisBaseFeatureStoreImpl
        <V extends IFeature, VF extends IFeatureStoreBase.FeatureField, F extends FeatureFilterBase<? super V>, T extends QueryBuilderBaseFeatureStore>
        extends PostgisStore<T>
        implements IFeatureStoreBase<V, VF, F> {

    private static final Logger logger = LoggerFactory.getLogger(PostgisBaseFeatureStoreImpl.class);

    protected final Lock lock = new ReentrantLock();

    public ThreadLocal<WKBWriter> threadLocalWriter = ThreadLocal.withInitial(WKBWriter::new);

    public ThreadLocal<PreparedStatement> batchPreparedStatement;

    protected static final int MAX_BATCH_SIZE = 10000;

    protected int currentBatchSize = 0;

    protected volatile Cache<FeatureKey, V> cache = CacheBuilder.newBuilder()
            .maximumSize(150_000)
            .softValues()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();

    protected SMLJsonBindings smlJsonBindings = new SMLJsonBindings(false);
    protected boolean useBatch;

    protected PostgisBaseFeatureStoreImpl(String url, String dbName, String login, String password, int idScope,
                                          IdProviderType dsIdProviderType, T queryBuilder, boolean useBatch){
        super(idScope,dsIdProviderType,queryBuilder);
        this.useBatch = useBatch;
        this.init(url, dbName, login, password, new String[]{
                queryBuilder.createTableQuery(),
                queryBuilder.createUidUniqueIndexQuery(),
                queryBuilder.createTrigramExtensionQuery(),
                queryBuilder.createTrigramDescriptionFullTextIndexQuery(),
                queryBuilder.createTrigramUidFullTextIndexQuery()
        });
    }

    @Override
    protected void init(String url, String dbName, String login, String password, String[] initScripts) {
        super.init(url, dbName, login, password, initScripts);
        if(useBatch) {
            batchPreparedStatement = ThreadLocal.withInitial(() -> {
                try {
                    PreparedStatement pstmt = batchConnection.prepareStatement(queryBuilder.addOrUpdateByIdQuery());
                    org.postgresql.PGStatement pgstmt = pstmt.unwrap(org.postgresql.PGStatement.class);
                    pgstmt.setPrepareThreshold(1);
                    return pstmt;
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Override
    public FeatureKey add(BigId parentID, V feature) throws DataStoreException {
        long id = idProvider.newInternalID(feature);
        Instant validInstant;
        if (feature.getValidTime() != null && feature.getValidTime().begin().getEpochSecond() > MIN_INSTANT.getEpochSecond()) {
            validInstant = feature.getValidTime().begin();
        } else {
            validInstant = Instant.MIN;
        }
        FeatureKey fk = new FeatureKey(BigId.fromLong(idScope, id), validInstant.truncatedTo(ChronoUnit.SECONDS));
        FeatureKey k = addOrUpdate(fk, parentID, feature);
        commit();
        return k;
    }

    public FeatureKey addOrUpdate(FeatureKey featureKey, BigId parentID, V value) throws DataStoreException {
        DataStoreUtils.checkFeatureObject(value);
        if(useBatch) {
            try {
                PreparedStatement preparedStatement = batchPreparedStatement.get();
                fillAddOrUpdateStatement(featureKey, parentID, value, preparedStatement);
                preparedStatement.addBatch();
                cache.invalidate(featureKey);
                if (currentBatchSize++ >= MAX_BATCH_SIZE) {
                    this.commit();
                }
            } catch (SQLException | IOException e) {
                throw new DataStoreException("Cannot insert feature " + value.getName());
            }
        } else {
            try (Connection connection = hikariDataSource.getConnection()) {
                try (PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.addOrUpdateByIdQuery())) {
                    fillAddOrUpdateStatement(featureKey, parentID, value, preparedStatement);
                    int rows = preparedStatement.executeUpdate();
                }
            } catch (SQLException | IOException e) {
                e.printStackTrace();
                throw new DataStoreException("Cannot insert feature " + value.getName());
            }
        }
        return featureKey;
    }

    private void fillAddOrUpdateStatement(FeatureKey featureKey, BigId parentID, V value,PreparedStatement preparedStatement) throws SQLException, IOException {
        preparedStatement.setLong(1, featureKey.getInternalID().getIdAsLong());
        preparedStatement.setString(6, value.getUniqueIdentifier());
        // statements for INSERT - parentId
       /* if (parentID != null && parentID != BigId.NONE) {
            preparedStatement.setLong(2, parentID.getIdAsLong());
            preparedStatement.setLong(6, parentID.getIdAsLong());
            preparedStatement.setLong(7, parentID.getIdAsLong());
        } else {
            preparedStatement.setLong(2, 0);
            preparedStatement.setLong(6, 0);
            preparedStatement.setLong(7, 0);
        }

        if (value.getGeometry() != null) {
            Geometry geometry = PostgisUtils.toJTSGeometry(value.getGeometry());
            byte[] geom = threadLocalWriter.get().write(geometry);
            preparedStatement.setBytes(3, geom); // insert
            preparedStatement.setBytes(8, geom); // update
        } else {
            preparedStatement.setNull(3, Types.LONGVARBINARY);
            preparedStatement.setNull(8, Types.LONGVARBINARY);
        }
        if (featureKey.getValidStartTime() != null && featureKey.getValidStartTime().getEpochSecond() > MIN_INSTANT.getEpochSecond()) {
            preparedStatement.setString(4, featureKey.getValidStartTime().toString());
            preparedStatement.setString(9, featureKey.getValidStartTime().toString());
        } else {
            preparedStatement.setString(4, "-infinity");
            preparedStatement.setString(9, "-infinity");
        }

        PGobject jsonObject = new PGobject();
        jsonObject.setType("json");
        jsonObject.setValue(this.writeFeature(value));

        preparedStatement.setObject(5, jsonObject);
        preparedStatement.setObject(10, jsonObject);
        */
        if (parentID != null && parentID != BigId.NONE) {
            preparedStatement.setLong(2, parentID.getIdAsLong());
        } else {
            preparedStatement.setLong(2, 0);
        }
        if (value.getGeometry() != null) {
            Geometry geometry = PostgisUtils.toJTSGeometry(value.getGeometry());
            byte[] geom = threadLocalWriter.get().write(geometry);
            preparedStatement.setBytes(3, geom); // insert
            preparedStatement.setBytes(7, geom); // update
        } else {
            preparedStatement.setNull(3, Types.LONGVARBINARY);
            preparedStatement.setNull(7, Types.LONGVARBINARY);
        }

        PGobject pgValidTimeRange = this.createPGobjectValidTimeRange(featureKey, value);
        preparedStatement.setObject(4, pgValidTimeRange);
        preparedStatement.setObject(8, pgValidTimeRange);

        PGobject jsonObject = new PGobject();
        jsonObject.setType("json");
        jsonObject.setValue(this.writeFeature(value));

        preparedStatement.setObject(5, jsonObject);
        preparedStatement.setObject(9, jsonObject);
    }

    public Stream<Entry<FeatureKey, V>> selectEntries(F filter, Set<VF> fields) {
        String queryStr = queryBuilder.createSelectEntriesQuery(filter, fields);
        logger.debug(queryStr);
        System.out.println(queryStr);
        List<Entry<FeatureKey, V>> results = new ArrayList<>();
        try (Connection connection = this.hikariDataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery(queryStr)) {
                    while (resultSet.next()) {
                        long id = resultSet.getLong("id");
                        String data = resultSet.getString("data");
                        V feature = this.readFeature(data);

                        PGobject pgRange = (PGobject) resultSet.getObject(String.valueOf(VALID_TIME));
                        Instant[] validTimeInstant = PostgisUtils.getInstantFromPGObject(pgRange);
                        FeatureKey featureKey = new FeatureKey(BigId.fromLong(idScope, id), validTimeInstant[0]);
                        results.add(Map.entry(featureKey, feature));
                    }
                }
            }
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
        return results.stream();
    }

    protected abstract V readFeature(String data) throws IOException;
    protected abstract String writeFeature(V feature) throws IOException;

    protected PGobject createPGobjectValidTimeRange(FeatureKey featureKey, V value) throws SQLException {
        PGobject range = new PGobject();
        range.setType("tsrange");  // type PostgreSQL
        String rangeValue;
        if (featureKey.getValidStartTime() != null && featureKey.getValidStartTime().getEpochSecond() > MIN_INSTANT.getEpochSecond()) {
            rangeValue ="["+PostgisUtils.checkAndGetValidInstant(value.getValidTime().begin())+","+
                    PostgisUtils.checkAndGetValidInstant(value.getValidTime().end())+"]";
        } else {
            rangeValue ="[-infinity,infinity]";
        }
        range.setValue(rangeValue);
        return range;
    }

    protected PGobject createPGobjectValidTimeRange(FeatureKey key) throws SQLException {
        PGobject range = new PGobject();
        range.setType("tsrange");  // type PostgreSQL
        String rangeValue;
        if (key.getValidStartTime() != null && key.getValidStartTime().getEpochSecond() > MIN_INSTANT.getEpochSecond()) {
            rangeValue ="["+PostgisUtils.checkAndGetValidInstant(key.getValidStartTime())+","+
                    PostgisUtils.checkAndGetValidInstant(key.getValidStartTime())+"]";
        } else {
            rangeValue ="[-infinity,infinity]";
        }

        range.setValue(rangeValue);
        return range;
    }

    @Override
    public V get(Object o) {
        if (!(o instanceof FeatureKey)) {
            throw new UnsupportedOperationException("Get operation is not supported with argument != FeatureKey key, got=" + o.getClass());
        }
        FeatureKey key = (FeatureKey) o;
        V feature = cache.getIfPresent(key);
        if (feature == null) {
            lock.lock();
            try {
                // double lock checking + volatile
                if (feature == null) {
                    try (Connection connection = hikariDataSource.getConnection()) {
                        try (PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.selectByPrimaryKeyQuery())) {
                            preparedStatement.setLong(1, key.getInternalID().getIdAsLong());
                            preparedStatement.setString(2, PostgisUtils.getPgTimestampFromInstant(key.getValidStartTime().truncatedTo(ChronoUnit.SECONDS)));
                            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                                if (resultSet.next()) {
                                    String data = resultSet.getString(1);
                                    feature = this.readFeature(data);
                                    cache.put(key, feature);
                                }
                            }
                        }
                    } catch (SQLException | IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            } finally {
                lock.unlock();
            }
        }
        return feature;
    }

    @Override
    public FeatureKey getCurrentVersionKey(BigId internalID) {
        try (Connection connection = hikariDataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                String query = queryBuilder.selectLastVersionByIdQuery(internalID.getIdAsLong(), Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
                try (ResultSet resultSet = statement.executeQuery(query)) {
                    if (resultSet.next()) {
                        long id = resultSet.getLong("id");
                        PGobject pgRange = (PGobject) resultSet.getObject("validTime");
                        return new FeatureKey(BigId.fromLong(idScope, id), PostgisUtils.getInstantFromPGObject(pgRange)[0]);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public FeatureKey getCurrentVersionKey(String uid) {
        try (Connection connection = hikariDataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                String query = queryBuilder.selectLastVersionByUidQuery(uid, Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
                try (ResultSet resultSet = statement.executeQuery(query)) {
                    if (resultSet.next()) {
                        long id = resultSet.getLong("id");

                        PGobject pgRange = (PGobject) resultSet.getObject("validTime");
                        return new FeatureKey(BigId.fromLong(idScope, id), PostgisUtils.getInstantFromPGObject(pgRange)[1]);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public V getCurrentVersion(BigId id) {
        FeatureKey key = this.getCurrentVersionKey(id);
        if (key != null) {
            return this.get(key);
        }
        return null;
    }

    @Override
    public V getCurrentVersion(String uid) {
        FeatureKey key = this.getCurrentVersionKey(uid);
        if (key != null) {
            return this.get(key);
        }
        return null;
    }

    protected void checkParentFeatureExists(BigId parentID) throws DataStoreException {
        DataStoreUtils.checkParentFeatureExists(this, parentID);
    }

    @Override
    @SuppressWarnings("unlikely-arg-type")
    public boolean contains(BigId internalID) {
        DataStoreUtils.checkInternalID(internalID);
        try (Connection connection = hikariDataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.existsByIdQuery())) {
                preparedStatement.setLong(1, internalID.getIdAsLong());
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet != null && resultSet.next()) {
                    return resultSet.getBoolean("exists");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return false;
    }


    @Override
    public boolean contains(String uid) {
        try (Connection connection = hikariDataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.existsByUidQuery())) {
                preparedStatement.setString(1, uid);
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet != null && resultSet.next()) {
                    return resultSet.getBoolean("exists");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    @Override
    public BigId getParent(BigId internalID) {
        return null;
    }

    @Override
    public Bbox getFeaturesBbox() {
        try (Connection connection = hikariDataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery(queryBuilder.selectExtentQuery())) {
                    if (resultSet.next()) {
                        PGbox2d pgBox = (PGbox2d) resultSet.getObject(QueryBuilderFeatureStore.EXTENT_COLUMN_NAME);
                        // minX,minY, maxX, maxY
                        return new Bbox(pgBox.getLLB().x, pgBox.getLLB().y, pgBox.getURT().x, pgBox.getURT().y);
                    } else {
                        throw new RuntimeException("No extent");
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    protected boolean existsUid(String uid) {
        try (Connection connection = hikariDataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.selectUidQuery())) {
                preparedStatement.setString(1, uid);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getInt("uidCount") > 0;
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    protected boolean existsId(long id) {
        try (Connection connection = hikariDataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.existsByIdQuery())) {
                preparedStatement.setLong(1, id);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getBoolean("exists");
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    @Override
    /**
     * Rules:
     * - if uid already exists, check version (associated time + id), if new, add feature with existing id but different validTime
     */
    public V put(FeatureKey featureKey, V value) {
        try {
            this.addOrUpdate(featureKey, null, value);
            return value;
        } catch (DataStoreException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public V remove(Object o) {
        if (!(o instanceof FeatureKey)) {
            throw new UnsupportedOperationException("Remove operation is not supported with argument != FeatureKey key, got=" + o.getClass());
        }
        FeatureKey key = (FeatureKey) o;
        V data = this.get(key);

        logger.debug("Remove Feature with key={}", key.toString());
        try (Connection connection = this.hikariDataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.removeByPrimaryKeyQuery())) {
                preparedStatement.setLong(1, key.getInternalID().getIdAsLong());

                PGobject pgValidTimeRange = this.createPGobjectValidTimeRange(key);
                preparedStatement.setObject(2, pgValidTimeRange);
                int rows = preparedStatement.executeUpdate();
                if (rows > 0) {
                    cache.invalidate(key);
                    return data;
                } else {
                    throw new RuntimeException("Cannot remove IFeature " + data.getName());
                }
            } catch (Exception ex) {
                throw ex;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long getNumFeatures() {
        try (Connection connection = this.hikariDataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery(queryBuilder.countFeatureQuery())) {
                    if (resultSet.next()) {
                        return resultSet.getLong("recordsCount");
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    @Override
    public boolean containsKey(Object key) {
        return this.get(key) != null;
    }

    public Collection<V> values() {
        return this.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toSet());
    }

    public Set<FeatureKey> keySet() {
        return this.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    public void clearCache() {
        cache.invalidateAll();
    }

    public void close() {
        try {
            if (batchPreparedStatement != null) {
                batchPreparedStatement.get().close();
            }
            if(batchConnection != null && !batchConnection.isClosed()) {
                batchConnection.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    @Override
    public void commit() throws DataStoreException {
        if (useBatch) {
            try {
                synchronized (batchPreparedStatement) {
                    if (currentBatchSize > 0) {
                        if (batchPreparedStatement != null) {
                            int[] rows = batchPreparedStatement.get().executeBatch();
//                        for(int i=0; i < rows.length; i++) {
//                            if(rows[i] == 0) {
//                                logger.error("Cannot commit");
//                                throw new DataStoreException("Cannot execute batch");
//                            }
//                        }
                        }
                        currentBatchSize = 0;
                    }
                }
            } catch (Exception ex) {
                //
                try {
                    batchConnection.rollback();
                } finally {
                    logger.error("Cannot commit", ex);
                    throw new DataStoreException("Cannot execute batch");
                }
            }
        }
    }
}
