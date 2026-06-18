package com.bachld.backend.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/** Body for bulk add/remove of students to/from a class. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StudentClassRequest {

    List<Integer> studentIds;
}
