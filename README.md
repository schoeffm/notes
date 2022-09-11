# notes

My personal version of [noted][noted]-CLI that is tailored to my habits of using a CLI tool - also this project acts as playground to get more accustomed to quarkus CLI applications (which is how this CLI tool was implemented).

Though, the basic idea stays the same - this tool acts as a quick way to create, add and search through notes. For more details on how that is supposed to be used see this [blog-post][note-taking-process]. Not sure yet if I have enough discipline to really stick to that idea - but it sounds so compelling to me to have more or less a quick lookup-diary of my daily things that I'd like to give it a try.

The CLI tool comes with a comprehensive help - compile it to a binary image for your OS and follow its help.

### Use Cases

My personal workday can be quite hectic with a bunch of unplanned interactions and meetings - sometimes I have to juggle with a bunch of information simultaneously. A life-saver so far was a clipboard history - and still, that information is ephemeral. Hence, I'd like to keep track of my actions and thus this note-taking approach _could_ be a solution ... let's see where that journey takes me.

```shell script
# this adds a new entry (augmented with timestamp)
notes add 'This is a headline

- first bullet point
- second bullet point
'

# just renders the current/today's note-file (using mdcat)
notes view

# this will create a new file `dedicated.md` within the notes-dir
notes add 'I do not think that belongs to my daily log' -f dedicated

# opens the current notes file in your default editor
notes edit

# uses grep -i to find all notes that contain the given search-string
notes search think that belong

# renders all markdown files to HTML
notes render

# lists the content of the notes-dir
notes ls
```

### Dependencies
The tool integrates with a bunch of other CLI-tools - so it was created in the assumption that you also have
- `grep`: in order to search through the notes
- `mdcat`: to render the respective markdown file in our terminal


[noted]:https://github.com/schoeffm/noted
[note-taking-process]:https://dev.to/scottshipp/my-note-taking-process-49pa
