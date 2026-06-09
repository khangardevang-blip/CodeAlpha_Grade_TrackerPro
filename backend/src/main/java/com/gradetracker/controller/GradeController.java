package com.gradetracker.controller;

import com.gradetracker.model.GradeRequest;
import com.gradetracker.model.GradeResponse;
import com.gradetracker.model.SubjectRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/grades")
@CrossOrigin(origins = "*")
public class GradeController {

    @PostMapping("/calculate")
    public GradeResponse calculateGrade(@RequestBody GradeRequest request) {
        double totalMarks = 0;
        
        if (request.getSubjects() != null) {
            for (SubjectRequest subject : request.getSubjects()) {
                totalMarks += subject.getMarks();
            }
        }

        int totalSubjects = request.getTotalSubjects() > 0 ? request.getTotalSubjects() : 1;
        
        // If the number of passed subjects is different from totalSubjects, 
        // we use the actual list size to avoid division by zero or incorrect percentage if they mismatch.
        int actualSubjects = request.getSubjects() != null ? request.getSubjects().size() : totalSubjects;
        if(actualSubjects == 0) actualSubjects = 1;
        
        double maxMarks = actualSubjects * 100.0;
        double averagePercentage = (totalMarks / maxMarks) * 100.0;

        String grade;
        if (averagePercentage >= 90) {
            grade = "A";
        } else if (averagePercentage >= 80) {
            grade = "B";
        } else if (averagePercentage >= 70) {
            grade = "C";
        } else if (averagePercentage >= 60) {
            grade = "D";
        } else {
            grade = "F";
        }

        GradeResponse response = new GradeResponse();
        response.setRollNumber(request.getRollNumber());
        response.setTotalMarks(totalMarks);
        response.setAveragePercentage(averagePercentage);
        response.setGrade(grade);
        response.setSubjects(request.getSubjects());

        return response;
    }
}
