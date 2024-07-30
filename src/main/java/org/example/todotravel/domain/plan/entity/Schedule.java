package org.example.todotravel.domain.plan.entity;

import lombok.*;

import jakarta.persistence.*;

@Entity
@Table(name = "schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id", nullable = false)
    private Long scheduleId;

    @Column(name = "status", nullable = false)
    private Boolean status;

    @Column(name = "travel_day_count", nullable = false)
    private Integer travelDayCount;

    @Column(name = "description")
    private String description;

    @ManyToOne
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan planId;

    @ManyToOne
    @JoinColumn(name = "location_id", nullable = false)
    private Location locationId;

    @ManyToOne
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicleId;

    @ManyToOne
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budgetId;
}
