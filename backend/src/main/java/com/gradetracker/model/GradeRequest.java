package com.gradetracker.model;

import java.util.List;

public class GradeRequest {
    private String rollNumber;
    private int totalSubjects;
    private List<SubjectRequest> subjects;

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public int getTotalSubjects() {
        return totalSubjects;
    }

    public void setTotalSubjects(int totalSubjects) {
        this.totalSubjects = totalSubjects;
    }

    public List<SubjectRequest> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<SubjectRequest> subjects) {
        this.subjects = subjects;
    }
}
