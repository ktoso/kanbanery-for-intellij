package com.intellij.tasks.kanbanery;

import com.google.common.collect.Lists;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.tasks.Task;
import com.intellij.tasks.TaskState;
import com.intellij.tasks.impl.BaseRepositoryImpl;
import com.intellij.tasks.kanbanery.model.KanbaneryComment;
import com.intellij.tasks.kanbanery.model.KanbaneryTask;
import com.intellij.util.xmlb.annotations.Tag;
import org.jetbrains.annotations.Nullable;
import pl.project13.janbanery.core.Janbanery;
import pl.project13.janbanery.core.JanbaneryFactory;
import pl.project13.janbanery.core.flow.TaskMarkFlow;
import pl.project13.janbanery.resources.Comment;
import pl.project13.janbanery.resources.User;

import java.io.IOException;
import java.util.List;

/**
 * @author Konrad Malawski
 */
@Tag("Kanbanery")
public class KanbaneryRepository extends BaseRepositoryImpl {

  private final static Logger LOG = Logger.getInstance("#com.intellij.tasks.kanbanery.KanbaneryRepository");

  private String projectName;
  private String workspaceName;

  private Janbanery janbanery;

  /**
   * for serialization
   */
  @SuppressWarnings({"UnusedDeclaration"})
  public KanbaneryRepository() {
    super();
  }

  private KanbaneryRepository(KanbaneryRepository other) {
    super(other);
  }

  public KanbaneryRepository(KanbaneryRepositoryType type) {
    super(type);
  }

  @Override
  public Task[] getIssues(String request, int max, long since) throws Exception {
    return getIssues();
  }

  private Task[] getIssues() throws IOException {
    List<pl.project13.janbanery.resources.Task> all = janbanery().tasks().all();

    List<KanbaneryTask> tasks = Lists.transform(all, KanbaneryTask.transformUsing(janbanery()));

    return tasks.toArray(new KanbaneryTask[tasks.size()]);
  }

  @Override
  public void setTaskState(Task task, TaskState state) throws Exception {
    pl.project13.janbanery.resources.Task target = new pl.project13.janbanery.resources.Task();
    target.setId(Long.valueOf(task.getId()));

    TaskMarkFlow mark = janbanery().tasks().mark(target);

    if (state == TaskState.IN_PROGRESS || state == TaskState.OPEN || state == TaskState.OTHER || state == TaskState.REOPENED) {
      mark.asNotReadyToPull();
    } else if (state == TaskState.RESOLVED) {
      mark.asReadyToPull();
    }
  }

  private Janbanery janbanery() {
    if (janbanery == null) {
      try {
        janbanery = new JanbaneryFactory().connectUsing(getUsername(), getPassword())
                                          .toWorkspace(workspaceName).usingProject(projectName);
      } catch (Exception e) {
        LOG.warn("Unable to login to Kanbanery...", e);
      }
    }

    return janbanery;
  }

  @Nullable
  @Override
  public Task findTask(String id) {
    try {
      pl.project13.janbanery.resources.Task task = janbanery().tasks().byId(Long.parseLong(id));
      User creator = janbanery().users().byId(task.getCreatorId());
      User owner = janbanery().users().byId(task.getOwnerId());

      List<Comment> comments = janbanery().comments().of(task).all();
      List<KanbaneryComment> kanbaneryComments = Lists.transform(comments, KanbaneryComment.transformUsing(janbanery()));

      return new KanbaneryTask(task, creator, owner, kanbaneryComments);
    } catch (Exception e) {
      LOG.warn("Cannot get issue " + id + ": " + e.getMessage());
      return null;
    }
  }

  @Override
  public KanbaneryRepository clone() {
    return new KanbaneryRepository(this);
  }

  public void setProject(String projectName) {
    this.projectName = projectName;
  }

  public void setWorkspaceName(String workspaceName) {
    this.workspaceName = workspaceName;
  }

  public void reloadJanbanery() {
    if (this.janbanery == null) {
      return;
    }

    janbanery.close();
    janbanery = null; // reset the lazy getter

    // create an instance using the new credentials / workspace
    janbanery();
  }
}
