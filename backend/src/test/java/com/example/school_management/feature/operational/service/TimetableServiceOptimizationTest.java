package com.example.school_management.feature.operational.service;

import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.academic.repository.CourseRepository;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.operational.entity.Period;
import com.example.school_management.feature.operational.entity.Room;
import com.example.school_management.feature.operational.entity.TimetableSlot;
import com.example.school_management.feature.operational.repository.PeriodRepository;
import com.example.school_management.feature.operational.repository.RoomRepository;
import com.example.school_management.feature.operational.repository.TimetableSlotRepository;
import com.example.school_management.feature.operational.service.impl.TimetableServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class TimetableServiceOptimizationTest {

    @Autowired
    private TimetableServiceImpl timetableService;

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private PeriodRepository periodRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private TimetableSlotRepository timetableSlotRepository;

    @Test
    public void testOptimizationWithValidData() {
        // This test verifies that optimization works with valid data
        // It will use the existing data from the database
        
        // Find a class that has courses
        List<ClassEntity> classes = classRepository.findAll();
        assertFalse(classes.isEmpty(), "No classes found in database");
        
        ClassEntity testClass = classes.get(0);
        System.out.println("Testing optimization for class: " + testClass.getName());
        
        // Check if class has courses
        assertFalse(testClass.getCourses().isEmpty(), "Class has no courses");
        System.out.println("Class has " + testClass.getCourses().size() + " courses");
        
        // Check if we have teachers
        List<Teacher> teachers = teacherRepository.findAll();
        assertFalse(teachers.isEmpty(), "No teachers found in database");
        System.out.println("Found " + teachers.size() + " teachers");
        
        // Check if we have periods
        List<Period> periods = periodRepository.findAllOrderByIndex();
        assertFalse(periods.isEmpty(), "No periods found in database");
        System.out.println("Found " + periods.size() + " periods");
        
        // Check if we have rooms
        List<Room> rooms = roomRepository.findAll();
        assertFalse(rooms.isEmpty(), "No rooms found in database");
        System.out.println("Found " + rooms.size() + " rooms");
        
        // Clear any existing slots for this class
        List<TimetableSlot> existingSlots = timetableSlotRepository.findByClassId(testClass.getId());
        if (!existingSlots.isEmpty()) {
            timetableSlotRepository.deleteAll(existingSlots);
            System.out.println("Cleared " + existingSlots.size() + " existing slots");
        }
        
        // Perform optimization
        timetableService.optimizeTimetableForClass(testClass.getId());
        
        // Verify that slots were created
        List<TimetableSlot> newSlots = timetableSlotRepository.findByClassId(testClass.getId());
        assertFalse(newSlots.isEmpty(), "No slots were created during optimization");
        System.out.println("Created " + newSlots.size() + " slots");
        
        // Verify slot properties
        for (TimetableSlot slot : newSlots) {
            assertNotNull(slot.getDayOfWeek(), "Slot should have a day of week");
            assertNotNull(slot.getPeriod(), "Slot should have a period");
            assertNotNull(slot.getForClass(), "Slot should have a class");
            assertNotNull(slot.getForCourse(), "Slot should have a course");
            assertNotNull(slot.getTeacher(), "Slot should have a teacher");
            assertNotNull(slot.getRoom(), "Slot should have a room");
            
            System.out.println("Slot: " + slot.getDayOfWeek() + " - Period " + 
                slot.getPeriod().getIndex() + " - " + slot.getForCourse().getName() + 
                " - " + slot.getTeacher().getFirstName() + " " + slot.getTeacher().getLastName() + 
                " - " + slot.getRoom().getName());
        }
        
        System.out.println("Optimization test completed successfully!");
    }
} 