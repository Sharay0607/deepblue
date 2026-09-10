package edu.coursehub.persistence.student;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "student_profile")
public class StudentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TODO-STUDENT S03: mapear Student con @OneToOne y @JoinColumn(student_id).

    @Column(length = 1000)
    private String biography;

    @Column(name = "github_url", length = 250)
    private String githubUrl;

    @Column(name = "linkedin_url", length = 250)
    private String linkedinUrl;

    protected StudentProfile() {
    }
}
