/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.store.feature;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.postgis.jdbc.PGbox2d;
import org.apache.commons.text.StringSubstitutor;
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
import org.sensorhub.impl.datastore.postgis.builder.QueryBuilderFeatureStore;
import org.sensorhub.impl.datastore.postgis.store.PostgisStore;
import org.sensorhub.impl.datastore.postgis.builder.IteratorResultSet;
import org.sensorhub.impl.datastore.postgis.builder.QueryBuilderBaseFeatureStore;
import org.sensorhub.impl.datastore.postgis.store.obs.PostgisObsStoreImpl;
import org.sensorhub.impl.datastore.postgis.utils.PostgisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.ogc.gml.IFeature;
import org.vast.sensorML.SMLJsonBindings;
import org.vast.util.Bbox;
import org.vast.util.TimeExtent;

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
import java.util.stream.StreamSupport;

import static org.sensorhub.api.datastore.feature.IFeatureStoreBase.FeatureField.VALID_TIME;
import static org.sensorhub.impl.datastore.postgis.utils.PostgisUtils.MIN_INSTANT;

public abstract class PostgisBaseFeatureStoreImpl
        <V extends IFeature, VF extends IFeatureStoreBase.FeatureField, F extends FeatureFilterBase<? super V>, T extends QueryBuilderBaseFeatureStore>
        extends PostgisStore<T>
        implements IFeatureStoreBase<V, VF, F> {

    private static final Logger logger = LoggerFactory.getLogger(PostgisBaseFeatureStoreImpl.class);
    protected final Lock lock = new ReentrantLock();
    public ThreadLocal<WKBWriter> threadLocalWriter = ThreadLocal.withInitial(WKBWriter::new);
    public static final int BATCH_SIZE = 500;
    private PostgisObsStoreImpl obsStore;

    protected volatile Cache<FeatureKey, V> cache = CacheBuilder.newBuilder()
            .maximumSize(50_000)
            .softValues()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();

    protected SMLJsonBindings smlJsonBindings = new SMLJsonBindings(false);

    protected PostgisBaseFeatureStoreImpl(String url, String dbName, String login, String password, int idScope,
                                          IdProviderType dsIdProviderType, T queryBuilder, boolean useBatch) {
        super(idScope, dsIdProviderType, queryBuilder, useBatch);
        this.init(url, dbName, login, password, new String[]{
                        queryBuilder.createTableQuery(),
                        queryBuilder.createUidUniqueIndexQuery(),
//                        queryBuilder.createValidTimeIndexQuery(),
                        queryBuilder.createIdIndexQuery(),
                        queryBuilder.createTrigramExtensionQuery(),
                        queryBuilder.createTrigramDescriptionFullTextIndexQuery(),
                        queryBuilder.createTrigramUidFullTextIndexQuery()
                }
        );
    }

    @Override
    protected void init(String url, String dbName, String login, String password, String[] initScripts) {
        super.init(url, dbName, login, password, initScripts);

        if(useBatch) {
            this.connectionManager.enableBatch(BATCH_SIZE);
        }
    }

    @Override
    public PostgisFeatureKey add(BigId parentID, V feature) throws DataStoreException {
        DataStoreUtils.checkFeatureObject(feature);
        checkParentFeatureExists(parentID);

        var existingKey = getCurrentVersionKey(feature.getUniqueIdentifier());
        if (existingKey != null) {
            if (parentID != null && existingKey.getParentID() != parentID.getIdAsLong())
                throw new DataStoreException("Feature is already associated to another parent");

            throw new DataStoreException(DataStoreUtils.ERROR_EXISTING_FEATURE_VERSION);
        }

        var newKey = generateKey(parentID.getIdAsLong(), existingKey, feature);
        addOrUpdate(newKey, parentID, feature);
        return newKey;
    }

    protected PostgisFeatureKey generateKey(long parentID, PostgisFeatureKey existingKey, V f) {
        // use existing IDs if feature is already known
        // otherwise generate new one
        BigId internalID;
        if (existingKey != null) {
            internalID = existingKey.getInternalID();
            parentID = existingKey.getParentID();
        } else {
            internalID = BigId.fromLong(idScope, idProvider.newInternalID(f));
        }

        // get valid start time from feature object
        // or use default value if no valid time is set
        Instant validStartTime;
        if (f.getValidTime() != null && f.getValidTime().begin().getEpochSecond() > MIN_INSTANT.getEpochSecond()) {
            validStartTime = f.getValidTime().begin();
        } else {
            validStartTime = Instant.MIN;
        }

        // generate full key
        return new PostgisFeatureKey(parentID, internalID, validStartTime);
    }

    public FeatureKey addOrUpdate(FeatureKey featureKey, BigId parentID, V value) throws DataStoreException {
        if (useBatch) {
            try {
                String sqlQuery = fillAddOrUpdateStatement(featureKey, parentID, value);
                this.connectionManager.addBatch(sqlQuery);
                this.connectionManager.tryCommit();
                cache.put(featureKey, value);
            } catch (Exception e) {
                throw new DataStoreException("Cannot insert feature " + value.getName());
            }
        } else {
            try (Connection connection = this.connectionManager.getConnection()) {
                try (Statement statement = connection.createStatement()) {
                    String sqlQuery = fillAddOrUpdateStatement(featureKey, parentID, value);
                    statement.executeUpdate(sqlQuery);
                }
            } catch (SQLException | IOException e) {
                e.printStackTrace();
                throw new DataStoreException("Cannot insert feature " + value.getName());
            }
        }
        return featureKey;
    }

    private String fillAddOrUpdateStatement(FeatureKey featureKey, BigId parentID, V value) throws SQLException, IOException {
        Map<String, Object> values = new HashMap<>();

        values.put("1","'"+featureKey.getInternalID().getIdAsLong()+"'::int8");
        // statements for INSERT - parentId
        var parentId = 0L;
        if(featureKey instanceof PostgisFeatureKey) {
            parentId = ((PostgisFeatureKey) featureKey).getParentID();
        } else if (parentID != null && parentID != BigId.NONE) {
            parentId = parentID.getIdAsLong();
        }

        values.put("2", "'"+parentId+"'::int8");
        if (value.getGeometry() != null) {
            Geometry geometry = PostgisUtils.toJTSGeometry(value.getGeometry());
            byte[] geom = threadLocalWriter.get().write(geometry);
            String byteaGeom = PostgisUtils.convertBytesToBytea(geom);
            values.put("3", "'"+byteaGeom+"'::bytea"); // insert
            values.put("6", "'"+byteaGeom+"'::bytea"); // update
        } else {
            values.put("3", "NULL");
            values.put("6", "NULL");
        }

        PGobject pgValidTimeRange = this.createPGobjectValidTimeRange(featureKey, value);
        values.put("4", "'"+pgValidTimeRange.getValue()+"'");
        values.put("7", "'"+pgValidTimeRange.getValue()+"'");

        String feature = this.writeFeature(value);
        values.put("5", "'"+feature+"'");
        values.put("8", "'"+feature+"'");
        StringSubstitutor sub = new StringSubstitutor(values);

        return sub.replace(queryBuilder.addOrUpdateByIdQuery());
    }

    public Stream<Entry<FeatureKey, V>> selectEntries(F filter, Set<VF> fields) {
        String queryStr = queryBuilder.createSelectEntriesQuery(filter, fields);
        if(logger.isDebugEnabled()) {
            logger.debug(queryStr);
        }
        IteratorResultSet<Entry<FeatureKey, V>> iteratorResultSet =
                new IteratorResultSet<>(
                        queryStr,
                        connectionManager,
                        STREAM_FETCH_SIZE,
                        (resultSet) -> resultSetToEntry(resultSet, fields),
                        (entry) -> (filter.getValuePredicate() == null || filter.getValuePredicate().test(entry.getValue())));
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iteratorResultSet, Spliterator.ORDERED), false);
    }

    private Entry<FeatureKey, V> resultSetToEntry(ResultSet resultSet, Set<VF> fields) {
        try {
            long id = resultSet.getLong("id");
            long parentId = resultSet.getLong("parentid");
            String data = resultSet.getString("data");
            V feature = this.readFeature(data);

            PGobject pgRange = (PGobject) resultSet.getObject(String.valueOf(VALID_TIME));
            Instant[] validTimeInstant = PostgisUtils.getInstantFromPGObject(pgRange);
            PostgisFeatureKey featureKey = new PostgisFeatureKey(parentId,BigId.fromLong(idScope, id), validTimeInstant[0]);
            return Map.entry(featureKey, feature);
        } catch (Exception ex) {
            throw new RuntimeException("Cannot parse resultSet to Feature", ex);
        }
    }
    protected abstract V readFeature(String data) throws IOException;

    protected abstract String writeFeature(V feature) throws IOException;

    protected PGobject createPGobjectValidTimeRange(FeatureKey featureKey, V value) throws SQLException {
        PGobject range = new PGobject();
        range.setType("tsrange");  // type PostgreSQL
        String startRangeValue;
        String endRangeValue;

        TimeExtent timeExtent = value.getValidTime();
        if (timeExtent == null) {
            startRangeValue = "-infinity";
            endRangeValue = "infinity";
        } else if (timeExtent.beginsNow()) {
            startRangeValue = "-infinity";
            endRangeValue = PostgisUtils.writeInstantToString(timeExtent.end().truncatedTo(ChronoUnit.SECONDS), false);
        } else if (timeExtent.endsNow()) {
            endRangeValue = "infinity";
            startRangeValue = PostgisUtils.writeInstantToString(timeExtent.begin().truncatedTo(ChronoUnit.SECONDS), false);
        } else {
            startRangeValue = PostgisUtils.writeInstantToString(timeExtent.begin().truncatedTo(ChronoUnit.SECONDS), false);
            endRangeValue = PostgisUtils.writeInstantToString(timeExtent.end().truncatedTo(ChronoUnit.SECONDS), false);
        }
        range.setValue("[" + startRangeValue + "," + endRangeValue + "]");
        return range;
    }

    protected PGobject createPGobjectValidTimeRange(FeatureKey key) throws SQLException {
        PGobject range = new PGobject();
        range.setType("tsrange");  // type PostgreSQL
        String rangeValue;
        if (key.getValidStartTime() != null && key.getValidStartTime().getEpochSecond() > MIN_INSTANT.getEpochSecond()) {
            String pgValidTime = PostgisUtils.writeInstantToString(key.getValidStartTime().truncatedTo(ChronoUnit.SECONDS), false);
            rangeValue = "[" + pgValidTime + "," + pgValidTime + "]";
        } else {
            rangeValue = "[-infinity,infinity]";
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
                    try (Connection connection = connectionManager.getConnection()) {
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
    public PostgisFeatureKey getCurrentVersionKey(BigId internalID) {
        try (Connection connection = connectionManager.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                String query = queryBuilder.selectLastVersionByIdQuery(internalID.getIdAsLong(), Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
                try (ResultSet resultSet = statement.executeQuery(query)) {
                    if (resultSet.next()) {
                        long id = resultSet.getLong("id");
                        PGobject pgRange = (PGobject) resultSet.getObject("validTime");
                        long parentid = resultSet.getLong("parentid");
                        return new PostgisFeatureKey(parentid,BigId.fromLong(idScope, id), PostgisUtils.getInstantFromPGObject(pgRange)[0]);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public PostgisFeatureKey getCurrentVersionKey(String uid) {
        try (Connection connection = connectionManager.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                String query = queryBuilder.selectLastVersionByUidQuery(uid, Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
                try (ResultSet resultSet = statement.executeQuery(query)) {
                    if (resultSet.next()) {
                        long id = resultSet.getLong("id");
                        PGobject pgRange = (PGobject) resultSet.getObject("validTime");
                        long parentId = resultSet.getLong("parentid");
                        return new PostgisFeatureKey(parentId,BigId.fromLong(idScope, id), PostgisUtils.getInstantFromPGObject(pgRange)[1]);
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
        if (cache.getIfPresent(internalID) != null)
            return true;
        try (Connection connection = connectionManager.getConnection()) {
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
        try (Connection connection = connectionManager.getConnection()) {
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
        try (Connection connection = connectionManager.getConnection()) {
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
        try (Connection connection = connectionManager.getConnection()) {
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
        try (Connection connection = connectionManager.getConnection()) {
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
            var fk = featureKey;
            // TODO: need to implements other subclass of feature
            if(featureKey instanceof PostgisFeatureKey) {
                var parentId = ((PostgisFeatureKey)featureKey).getParentID();
                this.addOrUpdate(featureKey, BigId.fromLong(idScope,parentId), value);
            } else {
                this.addOrUpdate(featureKey, null, value);
            }

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
        try (Connection connection = this.connectionManager.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.removeByPrimaryKeyQuery())) {
                preparedStatement.setLong(1, key.getInternalID().getIdAsLong());
//                PGobject pgValidTimeRange = this.createPGobjectValidTimeRange(key);
//                preparedStatement.setObject(2, pgValidTimeRange);
                preparedStatement.setString(2, PostgisUtils.getPgTimestampFromInstant(
                        key.getValidStartTime().truncatedTo(ChronoUnit.SECONDS)
                ));

                int rows = preparedStatement.executeUpdate();
                if (rows > 0) {
                    cache.invalidate(key);
                } else {
                    return null;
                }
            } catch (Exception ex) {
                throw ex;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return data;
    }

    @Override
    public long getNumFeatures() {
        try (Connection connection = this.connectionManager.getConnection()) {
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

    @Override
    protected void initUidHashIdProvider() {
        idProvider = DataStoreUtils.getFeatureHashIdProvider(212158449);
    }

    @Override
    public long removeEntries(F filter) {
        logger.debug("Remove Feature with filter={}", filter.toString());
        String queryStr = queryBuilder.createRemoveEntriesQuery(filter);
        System.out.println(queryStr);
        if(logger.isDebugEnabled()) {
            logger.debug(queryStr);
        }
        batchLock.lock();
        try (Connection connection = this.connectionManager.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                int rows = statement.executeUpdate(queryStr);
                if (rows > 0) {
                    // TODO: invalidate only concerned keys
                    cache.invalidateAll();
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        } finally {
            batchLock.unlock();
        }
    }
}
