package com.opencirc.api.passport.service.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencirc.api.passport.dto.CreatePassportUsingPlatformRequestDto;
import java.io.File;
import java.util.*;
import org.junit.jupiter.api.Test;

public class DebugSortTest {

  @Test
  public void testTopologicalSort() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    List<CreatePassportUsingPlatformRequestDto> dataArray =
        mapper.readValue(
            new File("test.json"),
            new TypeReference<List<CreatePassportUsingPlatformRequestDto>>() {});

    System.out.println("Input size: " + dataArray.size());

    List<CreatePassportUsingPlatformRequestDto> sorted = topologicalSort(dataArray);
    System.out.println("Output size: " + sorted.size());

    for (var data : sorted) {
      System.out.println("ID: " + data.getId() + ", Parent: " + data.getParentId());
    }
  }

  private List<CreatePassportUsingPlatformRequestDto> topologicalSort(
      List<CreatePassportUsingPlatformRequestDto> dataArray) {
    if (dataArray == null || dataArray.isEmpty()) {
      return dataArray;
    }

    Map<String, CreatePassportUsingPlatformRequestDto> idToDto = new HashMap<>();
    for (var data : dataArray) {
      if (data.getId() != null && !data.getId().isBlank()) {
        idToDto.put(data.getId(), data);
      }
    }

    Map<String, List<CreatePassportUsingPlatformRequestDto>> childrenMap = new HashMap<>();
    Map<CreatePassportUsingPlatformRequestDto, Integer> inDegree = new HashMap<>();

    for (var data : dataArray) {
      inDegree.put(data, 0);
    }

    for (var data : dataArray) {
      String parentId = data.getParentId();
      if (parentId != null && !parentId.isBlank() && idToDto.containsKey(parentId)) {
        childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(data);
        inDegree.put(data, inDegree.get(data) + 1);
      }
    }

    java.util.Queue<CreatePassportUsingPlatformRequestDto> queue = new java.util.LinkedList<>();
    for (var data : dataArray) {
      if (inDegree.get(data) == 0) {
        queue.add(data);
      }
    }

    List<CreatePassportUsingPlatformRequestDto> sortedData = new ArrayList<>();
    while (!queue.isEmpty()) {
      var current = queue.poll();
      sortedData.add(current);

      if (current.getId() != null && childrenMap.containsKey(current.getId())) {
        for (var child : childrenMap.get(current.getId())) {
          inDegree.put(child, inDegree.get(child) - 1);
          if (inDegree.get(child) == 0) {
            queue.add(child);
          }
        }
      }
    }

    if (sortedData.size() != dataArray.size()) {
      throw new RuntimeException(
          "Circular dependency detected! Sorted size: "
              + sortedData.size()
              + ", Expected: "
              + dataArray.size());
    }

    return sortedData;
  }
}
