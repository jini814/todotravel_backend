package org.example.todotravel.domain.plan.entity;

import lombok.*;

import jakarta.persistence.*;
import org.example.todotravel.domain.user.entity.User;

@Entity
@Table(name = "likes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Like {
    @Id
    @Column(name = "like_id", nullable = false)
    private String likeId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User likeUser;

    @ManyToOne
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan planId;
}
