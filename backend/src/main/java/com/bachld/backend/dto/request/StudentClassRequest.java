package com.bachld.backend.dto.request;

import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

/** Body for bulk add/remove of students to/from a class. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StudentClassRequest {

  List<Integer> studentIds;
}
