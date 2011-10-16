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
import com.google.common.collect.Lists;
import com.intellij.tasks.TaskState;
import com.intellij.tasks.TaskType;
import org.jetbrains.annotations.NotNull;
import pl.project13.janbanery.core.Janbanery;
import pl.project13.janbanery.resources.Comment;
import pl.project13.janbanery.resources.Task;
import pl.project13.janbanery.resources.User;

import javax.swing.*;
import java.util.Date;
import java.util.List;

/**
 * @author Konrad Malawski
 */
public class KanbaneryTask extends com.intellij.tasks.Task {

  private final Task task;
  private final List<KanbaneryComment> comments;

  public KanbaneryTask(Task task, User creator, User owner, List<KanbaneryComment> comments) {
    this.task = task;
    this.comments = comments;
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
    return comments.toArray(new KanbaneryComment[comments.size()]);
  }

  public Icon getIcon() {
//    String iconUrl = task.getTypeIconUrl();
    String iconUrl = "https://llp.kanbanery.com/images/icon-tasks.png";
//    return iconUrl == null ? null : isClosed() ? IconLoader.getDisabledIcon(iconUrl) : CachedIconLoader.getIcon(iconUrl);
    return null;
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
  public String getCustomIcon() {
    return null;
  }

  public static Function<Task, KanbaneryTask> transformUsing(final Janbanery janbanery) {

    return new Function<Task, KanbaneryTask>() {
      @Override
      public KanbaneryTask apply(Task task) {
        if (task == null) {
          return null;
        }

        List<Comment> comments = janbanery.comments().of(task).all();
        User creator = janbanery.users().byId(task.getCreatorId());
        User owner = janbanery.users().byId(task.getOwnerId());
        List<KanbaneryComment> kanbaneryComments = Lists.transform(comments, KanbaneryComment.transformUsing(janbanery));

        return new KanbaneryTask(task, creator, owner, kanbaneryComments);
      }
    };
  }
}
