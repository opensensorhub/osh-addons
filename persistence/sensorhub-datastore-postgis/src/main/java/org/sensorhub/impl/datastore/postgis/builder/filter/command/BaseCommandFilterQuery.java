package org.sensorhub.impl.datastore.postgis.builder.filter.command;

import org.sensorhub.api.datastore.TemporalFilter;
import org.sensorhub.api.datastore.command.CommandFilter;
import org.sensorhub.api.datastore.command.CommandStatusFilter;
import org.sensorhub.api.datastore.command.CommandStreamFilter;
import org.sensorhub.impl.datastore.postgis.builder.filter.FilterQuery;
import org.sensorhub.impl.datastore.postgis.builder.generator.FilterQueryGenerator;

import java.util.SortedSet;
import java.util.stream.Collectors;

public abstract class BaseCommandFilterQuery<F extends FilterQueryGenerator> extends FilterQuery<F> {
    protected BaseCommandFilterQuery(String tableName, F filterQueryGenerator) {
        super(tableName, filterQueryGenerator);
    }

    public F build(CommandFilter filter) {
        this.handleInternalIDs(filter.getInternalIDs());
        this.handleSenderIDs(filter.getSenderIDs());
        this.handleIssueTimeTemporalFilter(filter.getIssueTime());
        this.handleCommandStreamFilter(filter.getCommandStreamFilter());
        this.handleCommandStatusFilter(filter.getStatusFilter());
        return this.filterQueryGenerator;
    }

    protected void handleSenderIDs(SortedSet<String> ids) {
        if (ids != null && !ids.isEmpty()) {
            addCondition(this.tableName + ".sendId in (" +
                    ids.stream().collect(Collectors.joining(",")) +
                    ")");
        }
    }

    protected abstract void handleIssueTimeTemporalFilter(TemporalFilter temporalFilter);

    protected abstract void handleCommandStreamFilter(CommandStreamFilter commandStreamFilter);

    protected abstract void handleCommandStatusFilter(CommandStatusFilter commandStatusFilter);
}
