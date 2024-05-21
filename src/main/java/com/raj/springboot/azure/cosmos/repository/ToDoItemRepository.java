package com.raj.springboot.azure.cosmos.repository;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import com.raj.springboot.azure.cosmos.model.ToDoItem;

public interface ToDoItemRepository extends CosmosRepository<ToDoItem, Long> {


    ToDoItem findByName(String name);
}
