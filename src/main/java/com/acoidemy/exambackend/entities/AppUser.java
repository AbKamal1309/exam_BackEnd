package com.acoidemy.exambackend.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"exams", "testExams", "appRoles", "createdGroups", "memberGroups", "adminGroups"})
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    private String name;
    private String email;
    private String password;

    // ── Examens créés par l'utilisateur ───────────────────────────
    @OneToMany(mappedBy = "appUser", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Exam> exams = new ArrayList<>();


    // ── Tests passés par l'utilisateur ────────────────────────────
    // Un utilisateur peut passer PLUSIEURS tests (même examen plusieurs fois)
    @OneToMany(mappedBy = "appUser", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TestExam> testExams = new ArrayList<>();


    // ── Rôles ─────────────────────────────────────────────────────
    @ManyToMany(fetch = FetchType.EAGER)
    private Collection<AppRole> appRoles = new ArrayList<>();


    // ── Groupes créés par cet utilisateur (One-to-Many) ──────────
    // Un AppUser peut créer PLUSIEURS groupes
    @OneToMany(mappedBy = "creator", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Group> createdGroups = new ArrayList<>();

    // ── Groupes dont l'utilisateur est membre (Many-to-Many) ─────
    // Un AppUser peut être membre dans PLUSIEURS groupes
    @ManyToMany(mappedBy = "members", fetch = FetchType.LAZY)
    private List<Group> memberGroups = new ArrayList<>();

    // ── Groupes dont l'utilisateur est admin (Many-to-Many) ──────
    // Un AppUser peut être admin dans PLUSIEURS groupes
    // Seul le créateur du groupe peut lui attribuer ce rôle
    @ManyToMany(mappedBy = "admins", fetch = FetchType.LAZY)
    private List<Group> adminGroups = new ArrayList<>();

}