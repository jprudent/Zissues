Use github issues as a zettlekasten system. It's a simpler wiki

## How it works? 

The Github Web UI is used as a backend for writing, editing and consulting issues.
The mobile application can also be used. 

This backend is quite powerful:
- each issue is tagged either as resource or a reference card
- edition is done with markdown 
- the editor can upload image or screenshots right from the clipboard 
- the repo can be made private or public
- the repo can be collaborative 
- you can use a custom tagging system
- consultation of card is rich (tables, code syntax coloring, text formatting,..) 
- no need to install anything, available anywhere 
- cards can be searched by title, content, tags

I made an option tool suite that can export the system as a big self contained HTML file (images are embedded). 

You need to install [babashka](https://github.com/babashka/babashka).

This command will export all the issues in a self contained HTML file (could be heavy, and that's ugly for now)

> bb --classpath . -m zissues export-to-html --repo jprudent/jprudent.github.com --output-file /tmp/a.html




