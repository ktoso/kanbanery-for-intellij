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
import pl.project13.janbanery.exceptions.ProjectNotFoundException;
import pl.project13.janbanery.resources.Comment;
import pl.project13.janbanery.resources.Workspace;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Konrad Malawski
 */
@Tag("Kanbanery")
public class KanbaneryRepository extends BaseRepositoryImpl {

  private final static Logger LOG = Logger.getInstance("#com.intellij.tasks.kanbanery.KanbaneryRepository");

  private String myApiKey;

  private String myProjectName;
  private String myWorkspaceName;

  private Janbanery myJanbanery;

  /**
   * for serialization
   */
  @SuppressWarnings({"UnusedDeclaration"})
  public KanbaneryRepository() {
    super();
  }

  private KanbaneryRepository(KanbaneryRepository other) {
    super(other);
    this.myWorkspaceName = other.myWorkspaceName;
    this.myProjectName = other.myProjectName;
    this.myApiKey = other.myApiKey;
    this.myJanbanery = other.myJanbanery;
  }

  public KanbaneryRepository(KanbaneryRepositoryType type) {
    super(type);
  }

  @Override
  public Task[] getIssues(String request, int max, long since) throws Exception {
    return getIssues();
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

  private Task[] getIssues() throws IOException {
    List<pl.project13.janbanery.resources.Task> all = janbanery().tasks().all();

    List<KanbaneryTask> tasks = Lists.transform(all, KanbaneryTask.transformUsing(janbanery()));

    return tasks.toArray(new KanbaneryTask[tasks.size()]);
  }

  @Nullable
  @Override
  public Task findTask(String id) {
    try {
      pl.project13.janbanery.resources.Task task = janbanery().tasks().byId(Long.parseLong(id));

      List<Comment> comments = janbanery().comments().of(task).all();
      List<KanbaneryComment> kanbaneryComments = Lists.transform(comments, KanbaneryComment.transformUsing(janbanery()));

      return new KanbaneryTask(task, kanbaneryComments);
    } catch (Exception e) {
      LOG.warn("Cannot get issue " + id + ": " + e.getMessage());
      return null;
    }
  }

  Janbanery janbanery() {
    if (myJanbanery == null) {
      try {
        JanbaneryFactory.JanbaneryToWorkspace toWorkspace = new JanbaneryFactory().connectUsing(getLogin(), getPassword());
        if (myWorkspaceName.isEmpty() || myProjectName.isEmpty()) {
          myJanbanery = toWorkspace.notDeclaringWorkspaceYet();
        } else {
          try {
            myJanbanery = toWorkspace.toWorkspace(myWorkspaceName).usingProject(myProjectName);
          } catch (ProjectNotFoundException ex) {
            myJanbanery = toWorkspace.toWorkspace(myWorkspaceName);
          }
        }

      } catch (Exception e) {
        LOG.warn("Unable to login to Kanbanery...", e);
        return null;
      }
    }

    return myJanbanery;
  }

  public List<String> findDisplayableProjects() {
    List<Workspace> workspaces = getWorkspaces();
    List<String> displayableProjects = new ArrayList<String>();
    for (Workspace workspace : workspaces) {
      String workspaceName = workspace.getName();
      for (pl.project13.janbanery.resources.Project project : workspace.getProjects()) {
        String projectName = project.getName();

        displayableProjects.add(workspaceName + "/" + projectName);
      }
    }
    return displayableProjects;
  }

  @Override
  public KanbaneryRepository clone() {
    return new KanbaneryRepository(this);
  }

  public void setProject(String projectName) {
    this.myProjectName = projectName;
  }

  public void setWorkspaceName(String myWorkspaceName) {
    this.myWorkspaceName = myWorkspaceName;
  }

  public void reloadJanbanery() {
    closeJanbanery();

    // create an instance using the new credentials / workspace
    janbanery();
  }

  private void closeJanbanery() {
    if (this.myJanbanery != null) {
      myJanbanery.close();
      myJanbanery = null; // reset the lazy getter
    }
  }

  public boolean hasApiKey() {
    return myApiKey != null;
  }

  /**
   * Set new credentials and return true if they have changed (a refresh of projects will be requirec)
   */
  public boolean newCredentials(String user, String pass) {
    boolean changed = false;
    if (notEqual(myUsername, user) || notEqual(myPassword, pass)) {
      changed = true;
    }

    myUsername = user;
    myPassword = pass;

    myApiKey = null;

    if (changed) {
      closeJanbanery();
    }

    return changed;
  }

  public void useApiKey(String apiKey) {
    myUsername = null;
    myPassword = null;

    if (notEqual(myApiKey, apiKey)) {
      closeJanbanery();
    }

    myApiKey = apiKey;
  }

  private boolean notEqual(String oldValue, String user) {
    return !oldValue.equals(user);
  }

  public List<Workspace> getWorkspaces() {
    return janbanery().workspaces().all();
  }
}
