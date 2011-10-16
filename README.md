Kanbanery for Intellij
======================
It's a simple plugin to enable interaction between **<a href="http://www.kanbanery.com">Kanbanery</a>** and **JetBrains IntelliJ IDEA** Tasks.

Here's a demo video if you'd like to see it in action: http://vimeo.com/30644428 :-)

Kanbanery + IntelliJ + GitHub = Awesome
---------------------------------------
You can use this plugin, to automatically add task descriptions to your commits.
If you add the fact that Kanbanery can be made aware of github commits, you've got yourself a great mix of referencing tasks!
<img src="https://github.com/ktoso/kanbanery-for-intellij/raw/master/doc/commit_github_task_id_kanbanery.png" alt="git kanbanery intellij"/>

This is how switching tasks looks like (you can quickly open this window via **ALT + SHIFT + T**). Please note that you can use **CTRL + SPACE** to make the completion window to appear.
When you then press **CTRL + Q** (just as "show javadoc), you'll get more information about a task, such as it's description etc.
<img src="https://github.com/ktoso/kanbanery-for-intellij/raw/master/doc/switch_task_q_gives_a_nice_info.png"/>

Another thing it does is something like git stash, that is you can have different changesets between which you can switch.
It's called contexts and it's a feature of IntelliJ's Task system:
<img src="https://github.com/ktoso/kanbanery-for-intellij/raw/master/doc/" alt="tasks"/>

And that's how the settings menu looks like:
<img src="https://github.com/ktoso/kanbanery-for-intellij/raw/master/doc/settings_with_menu.png"/>


Janbanery, the fluent Kanbanery API Connector
=============================================
**Janbanery** is my Fluent Kanbanery API Wrapper. It made this plugin, and a lot more possible, such as...
the <a href="https://market.android.com/details?id=pl.project13.kanbanery&feature=search_result">Kanbanery for Android</a> client for example :-)

It's open source and being kept up to date as much as possible, you can clone it from this repo: <a href="https://github.com/ktoso/janbanery">https://github.com/ktoso/janbanery</a>

License
=======
I'm hereby releasing this plugin under the **Apache Software License 2.0**.
If you want to license it under a different license, just email me - I just would like to know how you would like to use it :-)

The full text of the license can be found along with the sources of this project.
