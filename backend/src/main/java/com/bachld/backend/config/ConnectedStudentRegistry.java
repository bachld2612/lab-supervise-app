package com.bachld.backend.config;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConnectedStudentRegistry {

    // classId → set of studentIds currently connected
    private final ConcurrentHashMap<Integer, Set<Integer>> classToStudents = new ConcurrentHashMap<>();

    public void register(Integer classId, Integer studentId) {
        classToStudents.computeIfAbsent(classId, k -> ConcurrentHashMap.newKeySet()).add(studentId);
    }

    public void unregister(Integer classId, Integer studentId) {
        Set<Integer> students = classToStudents.get(classId);
        if (students != null) {
            students.remove(studentId);
        }
    }

    public Set<Integer> getConnectedStudents(Integer classId) {
        return Collections.unmodifiableSet(
            classToStudents.getOrDefault(classId, Collections.emptySet())
        );
    }
}
