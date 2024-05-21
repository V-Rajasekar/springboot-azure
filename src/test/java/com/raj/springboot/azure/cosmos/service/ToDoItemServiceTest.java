package com.raj.springboot.azure.cosmos.service;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Optional;

import com.raj.springboot.azure.cosmos.model.ToDoItem;
import io.github.resilience4j.retry.RetryConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ComponentScan(basePackages = {"com.raj.springboot.azure.cosmos"})
public class ToDoItemServiceTest {

    @Autowired
    private ToDoItemService toDoItemService;

    @Autowired
    private RetryConfig retryConfig;

    @Test
    void testSaveTodoItem() {
        ToDoItem todoItem = new ToDoItem();
        todoItem.setId(Long.valueOf("1"));
        String name = "spring boot rest";
        todoItem.setName(name);
        todoItem.setCompleted(false);
        toDoItemService.saveToDoItem(todoItem);

        ToDoItem toDoItemSaved = toDoItemService.getToDoItem(name);
        System.out.println(" Etag:" + todoItem);
        toDoItemService.saveToDoItem(toDoItemSaved);
        ToDoItem toDoItemSaved2 = toDoItemService.getToDoItemById(todoItem.getId()).get();
        System.out.println(" Etag second:" + toDoItemSaved2);

        toDoItemService.saveToDoItem(toDoItemSaved);
        ToDoItem rt = toDoItemService.getToDoItem(name);
        System.out.println(" Etag second duplicate:" + rt);

        ToDoItem toDoItemSaved3 = toDoItemService.getToDoItem(name);
        toDoItemSaved3.setCompleted(true);
        toDoItemService.saveToDoItem(toDoItemSaved3);
        ToDoItem toDoItemSaved4 = toDoItemService.getToDoItem(name);
        System.out.println(" Etag third:" + toDoItemSaved4);
        toDoItemService.deleteToDoItem(todoItem.getId());
        Optional<ToDoItem> todoItemPresent = toDoItemService.getToDoItemById(todoItem.getId());
        assertFalse(todoItemPresent.isPresent());
    }
}
