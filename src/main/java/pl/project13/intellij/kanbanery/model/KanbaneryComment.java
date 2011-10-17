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

package pl.project13.intellij.kanbanery.model;

import com.google.common.base.Function;
import com.intellij.tasks.impl.SimpleComment;
import com.intellij.util.text.DateFormatUtil;
import pl.project13.janbanery.resources.Comment;
import pl.project13.janbanery.resources.User;

/**
 * @author Konrad Malawski
 */
public class KanbaneryComment extends SimpleComment {

  private final Comment comment;
  private final User author;

  public KanbaneryComment(Comment comment, User author) {
    super(comment.getCreatedAt().toDate(), author.getFirstName() + " " + author.getLastName(), comment.getBody());

    this.comment = comment;
    this.author = author;
  }

  @Override
  public void appendTo(StringBuilder builder) {
    builder.append("<hr>");
    builder.append("<table>");
    builder.append("<tr><td>");
    if (author.getGravatarUrl() != null) {
      builder.append("<img src=\"").append(author.getGravatarUrl()).append("?s=40\"/><br>");
    }
    builder.append("</td><td>");
    if (getAuthor() != null) {
      builder.append("<b>Author:</b> ").append(getAuthor()).append("<br>");
    }
    if (getDate() != null) {
      builder.append("<b>Date:</b> ").append(DateFormatUtil.formatDateTime(getDate())).append("<br>");
    }
    builder.append("</td></tr></table>");

    builder.append(getText()).append("<br>");
  }

  public static Function<Comment, KanbaneryComment> transformUsing() {
    return new Function<Comment, KanbaneryComment>() {
      @Override
      public KanbaneryComment apply(Comment input) {
        return new KanbaneryComment(input, new User.NoOne()); // todo change this
      }
    };
  }
}
