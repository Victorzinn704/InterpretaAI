package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Keeps completed session evidence while allowing the teacher to replace tomorrow's roster. */
public final class V22__Preserve_classroom_roster_history extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    alter table institution_classroom_learner
                        add column roster_version integer not null default 1
                    """);
            statement.execute("""
                    alter table classroom_session
                        add column roster_version integer not null default 1
                    """);
        }

        dropUniqueConstraint(connection, List.of("classroom_id", "learner_alias"));
        dropUniqueConstraint(connection, List.of("classroom_id", "seat_number"));

        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    alter table institution_classroom_learner
                        add constraint uq_classroom_roster_alias
                        unique (classroom_id, roster_version, learner_alias)
                    """);
            statement.execute("""
                    alter table institution_classroom_learner
                        add constraint uq_classroom_roster_seat
                        unique (classroom_id, roster_version, seat_number)
                    """);
            statement.execute("""
                    create index idx_classroom_learner_roster
                        on institution_classroom_learner
                        (classroom_id, roster_version, seat_number)
                    """);
        }
    }

    private static void dropUniqueConstraint(Connection connection, List<String> columns)
            throws Exception {
        Map<String, List<String>> constraints = new LinkedHashMap<>();
        try (PreparedStatement query = connection.prepareStatement("""
                select tc.constraint_name, kcu.column_name
                  from information_schema.table_constraints tc
                  join information_schema.key_column_usage kcu
                    on tc.constraint_catalog = kcu.constraint_catalog
                   and tc.constraint_schema = kcu.constraint_schema
                   and tc.constraint_name = kcu.constraint_name
                 where lower(tc.table_name) = 'institution_classroom_learner'
                   and tc.constraint_type = 'UNIQUE'
                 order by tc.constraint_name, kcu.ordinal_position
                """); ResultSet result = query.executeQuery()) {
            while (result.next()) {
                constraints.computeIfAbsent(result.getString(1), ignored -> new ArrayList<>())
                        .add(result.getString(2).toLowerCase(Locale.ROOT));
            }
        }
        String constraint = constraints.entrySet().stream()
                .filter(entry -> entry.getValue().equals(columns))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Unique constraint not found for " + columns));
        String quote = connection.getMetaData().getIdentifierQuoteString().strip();
        String quoted = quote + constraint.replace(quote, quote + quote) + quote;
        try (Statement statement = connection.createStatement()) {
            statement.execute("alter table institution_classroom_learner drop constraint " + quoted);
        }
    }
}
