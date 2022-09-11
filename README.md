# notes

My personal version of [noted][noted]-CLI that is tailored to my habits of using a CLI tool - also this project acts as playground to get more accustomed to quarkus CLI applications (which is how this CLI tool was implemented).

Though, the basic idea stays the same - this tool acts as a quick way to create, add and search through notes. For more details on how that is supposed to be used see this [blog-post][note-taking-process]. Not sure yet if I have enough discipline to really stick to that idea - but it sounds so compelling to me to have more or less a quick lookup-diary of my daily things that I'd like to give it a try.

The CLI tool comes with a comprehensive help - compile it to a binary image for your OS and follow its help.

### Dependencies
The tool integrates with a bunch of other CLI-tools - so it was created in the assumption that you also have
- `grep`: in order to search through the notes


[noted]:https://github.com/schoeffm/noted
[note-taking-process]:https://dev.to/scottshipp/my-note-taking-process-49pa
