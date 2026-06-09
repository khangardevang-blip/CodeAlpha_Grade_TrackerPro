package com.gradetracker.model;

import java.util.List;

public class GradeResponse {
    private String rollNumber;
    private double totalMarks;
    private double averagePercentage;
    private String grade;
    private List<SubjectRequest> subjects;

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public double getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(double totalMarks) {
        this.totalMarks = totalMarks;
    }

    public double getAveragePercentage() {
        return averagePercentage;
    }

    public void setAveragePercentage(double averagePercentage) {
        this.averagePercentage = averagePercentage;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public List<SubjectRequest> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<SubjectRequest> subjects) {
        this.subjects = subjects;
    }
}
