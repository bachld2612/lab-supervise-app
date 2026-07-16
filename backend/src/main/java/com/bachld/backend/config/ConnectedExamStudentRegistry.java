package com.bachld.backend.config;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConnectedExamStudentRegistry {

  // examRoomId → set of studentIds currently connected
  ConcurrentHashMap<Integer, Set<Integer>> examRoomToStudents = new ConcurrentHashMap<>();

  // studentId → examRoomId they connected to (for post-exam disconnect)
  ConcurrentHashMap<Integer, Integer> studentToExamRoom = new ConcurrentHashMap<>();

  public void register(Integer examRoomId, Integer studentId) {
    examRoomToStudents
        .computeIfAbsent(examRoomId, k -> ConcurrentHashMap.newKeySet())
        .add(studentId);
    studentToExamRoom.put(studentId, examRoomId);
  }

  public void unregister(Integer examRoomId, Integer studentId) {
    Set<Integer> students = examRoomToStudents.get(examRoomId);
    if (students != null) {
      students.remove(studentId);
    }
    studentToExamRoom.remove(studentId);
  }

  public Set<Integer> getConnectedStudents(Integer examRoomId) {
    return Collections.unmodifiableSet(
        examRoomToStudents.getOrDefault(examRoomId, Collections.emptySet()));
  }

  /** Returns the exam room ID the student is currently registered in, or null if not registered. */
  public Integer getExamRoomForStudent(Integer studentId) {
    return studentToExamRoom.get(studentId);
  }
}
