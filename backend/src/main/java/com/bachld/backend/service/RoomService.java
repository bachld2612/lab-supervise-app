package com.bachld.backend.service;

import com.bachld.backend.dto.request.RoomCreateRequest;
import com.bachld.backend.dto.request.RoomUpdateRequest;
import com.bachld.backend.dto.response.RoomResponse;
import com.bachld.backend.model.Room;
import com.bachld.backend.repository.RoomRepository;
import com.bachld.backend.util.enums.Status;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomService {

  RoomRepository roomRepository;

  public Page<RoomResponse> getList(Pageable pageable, String keyword, Integer status) {
    if (keyword != null) {
      keyword = "%" + keyword.trim().toLowerCase() + "%";
    } else {
      keyword = "%%";
    }
    return roomRepository.findByKeyword(pageable, keyword, status);
  }

  public RoomResponse getById(Integer id) {
    return roomRepository.findByIdAndStatus(id, Status.ACTIVE.getValue());
  }

  public void create(RoomCreateRequest request) {
    Room room = new Room();
    room.setName(request.getName());
    room.setCapacity(request.getCapacity());
    room.setStatus(Status.ACTIVE.getValue());
    roomRepository.save(room);
  }

  public void update(RoomUpdateRequest request, int id) {
    Room room =
        roomRepository
            .findById(id)
            .orElseThrow(
                () -> new IllegalArgumentException("Không tìm thấy phòng học có id: " + id));

    if (request.getName() != null && !request.getName().isEmpty()) {
      room.setName(request.getName());
    }
    if (request.getCapacity() != null) {
      room.setCapacity(request.getCapacity());
    }
    roomRepository.save(room);
  }

  public void deleteById(Integer id) {
    Room room =
        roomRepository
            .findById(id)
            .orElseThrow(
                () -> new IllegalArgumentException("Không tìm thấy phòng học có id: " + id));
    room.setStatus(Status.INACTIVE.getValue());
    roomRepository.save(room);
  }
}
