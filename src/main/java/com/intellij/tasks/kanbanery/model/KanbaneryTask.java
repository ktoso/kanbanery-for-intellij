/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.intellij.tasks.kanbanery.model;

import com.google.common.base.Function;
import com.intellij.openapi.util.IconLoader;
import com.intellij.tasks.Comment;
import com.intellij.tasks.TaskState;
import com.intellij.tasks.TaskType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.project13.janbanery.resources.Task;

import javax.swing.*;
import java.util.Date;
import java.util.Map;

/**
 * @author Konrad Malawski
 */
public class KanbaneryTask extends com.intellij.tasks.Task {

  private final Task task;

  public KanbaneryTask(Task task, pl.project13.janbanery.resources.TaskType taskType) {
    this.task = task;
    this.task.setTaskTypeName(taskType.getName());
  }

  @NotNull
  public String getId() {
    return String.valueOf(task.getId());
  }

  @NotNull
  public String getSummary() {
    return task.getTitle();
  }

  public String getDescription() {
    return task.getDescription();
  }

  @NotNull
  public com.intellij.tasks.Comment[] getComments() {
    return Comment.EMPTY_ARRAY;
  }

  public Icon getIcon() {
    return IconLoader.getIcon("/resources/kanbanery.png");
  }

  @NotNull
  @Override
  public TaskType getType() {
    String type = task.getType();
    if (type == null) {
      return TaskType.OTHER;
    } else if (type.equals("Bug")) {
      return TaskType.BUG;
    } else if (type.equals("Exception")) {
      return TaskType.EXCEPTION;
    } else if (type.equals("New Feature")) {
      return TaskType.FEATURE;
    } else {
      return TaskType.OTHER;
    }
  }

  @Override
  public TaskState getState() {
    if (task.getReadyToPull()) {
      return TaskState.RESOLVED;
    } else {
      return TaskState.OPEN;
    }
  }

  @NotNull
  @Override
  public Date getUpdated() {
    return task.getUpdatedAt().toDate();
  }

  @Override
  public Date getCreated() {
    return task.getCreatedAt().toDate();
  }

  @Override
  public boolean isClosed() {
    return false;
  }

  public boolean isIssue() {
    return true;
  }

  @Override
  public String getIssueUrl() {
    return String.format("https://kanbanery.com/tasks/%d/in-context", task.getId());
  }

  @Override
  @Nullable
  public String getCustomIcon() {
    return null;
  }

  @Override
  public String getPresentableName() {
    return String.format("[#%d] (%s): %s", task.getId(), task.getTaskTypeName(), task.getTitle());
  }

  public static Function<Task, KanbaneryTask> transform(final Map<Long, pl.project13.janbanery.resources.TaskType> taskTypes) {

    return new Function<Task, KanbaneryTask>() {
      @Override
      public KanbaneryTask apply(Task task) {
        if (task == null) {
          return null;
        }

        pl.project13.janbanery.resources.TaskType taskType = taskTypes.get(task.getTaskTypeId());
        return new KanbaneryTask(task, taskType);
      }
    };
  }
}