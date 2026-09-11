package com.deepblue.rescue.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "specialists")
public class Specialist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "professional_code", nullable = false, unique = true, length = 50)
    private String professionalCode;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @ManyToMany
    @JoinTable(
            name = "specialist_expertise",
            joinColumns = @JoinColumn(name = "specialist_id"),
            inverseJoinColumns = @JoinColumn(name = "expertise_id")
    )
    private Set<Expertise> expertiseAreas = new HashSet<>();

    @OneToMany(mappedBy = "specialist")
    private List<Treatment> treatments = new ArrayList<>();

    protected Specialist() {
    }

    public Specialist(String professionalCode,
                      String firstName,
                      String lastName,
                      String email) {
        this.professionalCode = professionalCode;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.active = true;
    }

    public void addExpertise(Expertise expertise) {
        expertiseAreas.add(expertise);
        expertise.getSpecialists().add(this);
    }

    public void removeExpertise(Expertise expertise) {
        expertiseAreas.remove(expertise);
        expertise.getSpecialists().remove(this);
    }

    public Long getId() {
        return id;
    }

    public String getProfessionalCode() {
        return professionalCode;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Set<Expertise> getExpertiseAreas() {
        return expertiseAreas;
    }

    public List<Treatment> getTreatments() {
        return treatments;
    }
}