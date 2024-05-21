package com.raj.springboot.azure.cosmos.service;

import java.util.Optional;

import com.azure.spring.data.cosmos.exception.CosmosAccessException;
import com.raj.springboot.azure.cosmos.exception.CosmosRetryException;
import com.raj.springboot.azure.cosmos.model.ToDoItem;
import com.raj.springboot.azure.cosmos.repository.ToDoItemRepository;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ToDoItemService {

    private static final Logger LOG = LoggerFactory.getLogger(ToDoItemService.class);
    private ToDoItemRepository todoRepo;

    private final Retry retry;
    public ToDoItemService(ToDoItemRepository todoRepo,  @Qualifier("retryConfig") RetryConfig retryConfig) {
        this.todoRepo = todoRepo;
        RetryRegistry retryRegistry = RetryRegistry.of(retryConfig);
        retry = retryRegistry.retry("todoItemService", retryConfig);

        retry.getEventPublisher().onRetry(e -> LOG.info("Retry-operation for {}, attempt# {}", "TODO",
                e.getNumberOfRetryAttempts()));
        retry.getEventPublisher().onSuccess(e -> LOG.info("Retry-operation successful, for {}", "TODO"));
        retry.getEventPublisher().onError(e -> LOG.error("Retry-operation failed, for {}", "TODO"));
    }

    /**
     * Method to save todoItem with retry option. This method decorate the save method with a retry
     * config to retry conflict saves(i.e) item changed after read, and before update is performed on that item, an
     * cosmosException is thrown with error code 429, which is retried again see #CosmosDBConfiguration.getRetryConfig
     * @param todoItem
     */
    public void saveToDoItem(ToDoItem todoItem) {
        try {
            if (null == todoItem) {
                return;
            }
            Retry.decorateRunnable(retry, () -> todoRepo.save(todoItem)).run();
            LOG.debug("Todo saved, {}", todoItem.getName());
        } catch (CosmosAccessException cosmosAccessException) {
            if (cosmosAccessException.getCosmosException().getStatusCode() == 429) {
                throw new CosmosRetryException("Exception occurred while saving document",
                        cosmosAccessException);
            }
            throw cosmosAccessException;
        }
    }

    public ToDoItem getToDoItem(String todoItemName) {
       return todoRepo.findByName(todoItemName);
    }

    public Optional<ToDoItem> getToDoItemById(Long id) {
        return todoRepo.findById(id);
    }
    public void deleteToDoItem(Long id) {
        todoRepo.deleteById(id);
    }
}
