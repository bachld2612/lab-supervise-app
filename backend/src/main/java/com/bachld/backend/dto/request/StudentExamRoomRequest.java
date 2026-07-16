package com.bachld.backend.dto.request;

import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

/** Body for bulk add/remove of students to/from an exam room. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StudentExamRoomRequest {

  List<Integer> studentIds;
}
