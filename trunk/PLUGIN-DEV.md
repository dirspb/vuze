This is a new *work-in-progress* guide on how to write a plugin for
Azureus.

# Introduction

## What things can a plugin do?

A lot of things. :) For something to technically be a plugin, it just
needs to do a couple of things so that Azureus recognises it as one.

Plugins are essentially individual programs which are provided with a
mechanism to interact with Azureus. They are Java programs which define
an "entry" point to allow Azureus to use it as a plugin. So when we say
plugins can do anything - generally speaking, they can. They have the
whole of the [Java API](http://java.sun.com/j2se/1.5.0/docs/api/) to
interact with.

However, what plugins can do with Azureus is limited to the [API's
provided](http://azureus.sourceforge.net/plugins/doc/). Plugins can add
table columns, menu items, present new views of information, generate
config sections, manage downloads (including adding new ones), change
settings, write extension messages to communicate with other peers,
interact with other plugins - doing a lot of things which Azureus does).

Azureus doesn't just provide a large number of methods to interact with
it, it also provides a large number of convenient utilities - like the
ability to prompt the user for information (regardless of what interface
the user might be using), a [generic downloader
facility](http://azureus.sourceforge.net/plugins/doc/org/gudy/azureus2/plugins/utils/resourcedownloader/ResourceDownloaderFactory.html)
(being able to download from HTTP, handling authentication, downloading
from BitTorrent, specifying retry mechanisms)...

## How do I know what's available to me?

The best guide at the moment is the
[Javadoc](http://azureus.sourceforge.net/plugins/doc/) documentation
that is available. The best approach to have is to decide what you want
your plugin to do, and then see if Azureus provides you a way to do it.
You can also [post in the
forum](http://forum.vuze.com/forum.jspa?forumID=7&start=0) if you have
any questions.

## What if Azureus doesn't provide me with a way to do what I want?

Again, [on the
forum](http://forum.vuze.com/forum.jspa?forumID=7&start=0). Since I
started writing plugins in Azureus, I have extended the plugin API so
that plugins have a lot more control over many things. If you think the
plugin API is lacking anything (either minor or major changes), you can
make requests in the forum to be added.

# How do I get started?

## Prerequisites

To get started, you will need to be able to program in Java and have a
decent Java editor - if you're not familiar with writing Java programs,
you will probably struggle. If you've programmed in an object-orientated
language before, you might find that it's not too difficult to program
in Java.

As for choice of Java IDE to use - I'll leave that you. A lot of the
Azureus developers use [Eclipse](http://eclipse.org/) (you can see a
guide of how to start modifying the Azureus source with Eclipse [
here](Using_Eclipse "wikilink")).

## Dependencies

When you write your plugin, you will need to compile against the Azureus
code itself, which is provided in a single JAR file. You need to use
either the main `Azureus2.jar` file or a JAR file containing just the
interfaces which make up the plugin API (both of which are available
[here](http://sourceforge.net/project/showfiles.php?group_id=84122)).

If you write any plugins which need to interact with SWT objects, then
you will need to use the `swt.jar` file that is in the Azureus directory
too.

# Step-by-step guide

This section describes gradual development of an example plugin, which
includes more and more functionality. It's not necessary for you to
follow all steps to know how to write a plugin, but later sections may
refer to previous sections.

Rather than naming the plugin something like "ExamplePlugin", I've
called it [Aizen](http://en.wikipedia.org/wiki/S%C5%8Dsuke_Aizen),
because Aizen wears glasses, and you can trust a man with glasses...

## Getting prepared

1.  [ Preparing an environment in
    Eclipse.](PluginDevGuide_Eclipse "wikilink")
2.  [ Writing a blank plugin.](PluginDevGuide_Creating "wikilink")
3.  [ Running the plugin.](PluginDevGuide_Running "wikilink")
4.  [ Building the plugin.](PluginDevGuide_Building "wikilink")

## The basics

1.  [ How plugins work with
    Azureus.](PluginDevGuide_PluginInterface "wikilink")
2.  [ Storing data and changing config
    settings.](PluginDevGuide_PluginConfig "wikilink")
3.  [ Defining plugin
    text.](PluginDevGuide_Internationalisation "wikilink")
4.  [ Creating a configuration
    page.](PluginDevGuide_ConfigurationPage "wikilink")
5.  [ Alerts and logging.](PluginDevGuide_Logging "wikilink")

## User Interface (basic)

1.  [ Adding menus.](PluginDevGuide_Menus "wikilink")
2.  [ Adding table columns.](PluginDevGuide_TableColumns "wikilink")

## Downloads

1.  [ Adding download listeners.](PluginDevGuide_Listeners "wikilink")
2.  [ Manipulating download
    objects.](PluginDevGuide_Downloads "wikilink")
3.  [ Manipulating torrents.](PluginDevGuide_Torrents "wikilink")
4.  [ Storing values on objects.](PluginDevGuide_Attributes "wikilink")
5.  [ Adding torrents and
    downloads.](PluginDevGuide_AddDownload "wikilink")

## General

1.  [ More uses of listener
    objects.](PluginDevGuide_ListenersPlusPlus "wikilink")
2.  [ The utilities library.](PluginDevGuide_Utilities "wikilink")

## User Interface (advanced)

1.  [ More interesting things to do with menus and
    tables.](PluginDevGuide_AdvancedMenusAndTables "wikilink")
2.  [ Getting text input and sending messages to
    users.](PluginDevGuide_UserInput "wikilink")
3.  [ Providing interface-specific
    behaviour.](PluginDevGuide_UISpecific "wikilink")
4.  [ Creating a status entry.](PluginDevGuide_StatusEntry "wikilink")
5.  [ Creating log views and using
    views.](PluginDevGuide_UIViews "wikilink")

## Advanced

1.  [ Threading.](PluginDevGuide_Threading "wikilink")
2.  [ Generic resource downloading /
    uploading.](PluginDevGuide_ResourceFactory "wikilink")
3.  [ Extension messaging - sending messages between
    peers.](PluginDevGuide_ExtMessaging "wikilink")
4.  [ Plugin-to-plugin communication.](PluginDevGuide_IPC "wikilink")

[Category:Technical
Information](Category:Technical_Information "wikilink") [Category:Plugin
Development](Category:Plugin_Development "wikilink")