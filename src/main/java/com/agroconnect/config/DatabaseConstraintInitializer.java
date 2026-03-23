package com.agroconnect.config;

import com.agroconnect.model.DeliveryTask;
import com.agroconnect.model.Harvest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DatabaseConstraintInitializer {
    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void refreshDeliveryTaskStatusConstraint() {
        ensureDemandChangeRequestColumns();
        refreshEnumConstraint("delivery_tasks", "delivery_tasks_status_check", DeliveryTask.Status.values());
        refreshEnumConstraint("harvests", "harvests_status_check", Harvest.Status.values());
    }

    private void ensureDemandChangeRequestColumns() {
        jdbcTemplate.execute("ALTER TABLE demands ADD COLUMN IF NOT EXISTS requested_quantity double precision");
        jdbcTemplate.execute("ALTER TABLE demands ADD COLUMN IF NOT EXISTS requested_required_date date");
        jdbcTemplate.execute("ALTER TABLE demands ADD COLUMN IF NOT EXISTS requested_target_price double precision");
        jdbcTemplate.execute("ALTER TABLE demands ADD COLUMN IF NOT EXISTS change_request_reason varchar(255)");
    }

    private void refreshEnumConstraint(String tableName, String constraintName, Enum<?>[] values) {
        String allowedStatuses = Arrays.stream(values)
                .map(status -> "'" + status.name() + "'")
                .collect(Collectors.joining(", "));

        jdbcTemplate.execute("ALTER TABLE " + tableName + " DROP CONSTRAINT IF EXISTS " + constraintName);
        jdbcTemplate.execute(
                "ALTER TABLE " + tableName + " ADD CONSTRAINT " + constraintName +
                        " CHECK (status IN (" + allowedStatuses + "))"
        );
    }
}
