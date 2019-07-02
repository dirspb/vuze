This page gives a step by step description of running the Vuze/Azureus
BitTorrent client in the [Eclipse](http://eclipse.org/) IDE. This has
been tested with Vuze 4.6.0.5 source code and Eclipse 3.7 (Indigo).

*Note:* If you're familiar with Eclipse and want to do everything your
own way the [Project](#Configure_the_Project "wikilink") and
[Run](#Configure_the_Run_dialog "wikilink") chapters will be interesting
for you

*Note:* This article was almost completely rewritten in April 2010 to
reflect the change to the SVN source code repository (from the old CVS).

## Installing Eclipse

1.  Download [Eclipse](http://eclipse.org/downloads/index.php) and
    extract the archive to a directory of your choice
2.  Start the Eclipse launcher (the actual file name depends on the OS)
    and choose a location for the workspace (the default path should be
    ok)
3.  Under *Window \> Preferences*, tweak the following settings:
      - *Java \> Build Path*: select *Folders* and clear the *source
        folder* box and make sure "bin" is in the *output folder* box.
4.  click on *OK* and go to the *Project* Menu and unselect *Build
    Automatically* (Eclipse will build the project anyway when you use
    the Run option)

## Set up SVN Functionality

To load Vuze into Eclipse for editing, the first thing you should do is
set up SVN client functionality. That can be be done easiest by
[installing
Subclipse](http://subclipse.tigris.org/servlets/ProjectProcess?pageID=p4wYuA).
Subclipse will add SVN functionality to Eclipse. It can be installed
through Eclipse's update system.

  - **When installing Subclipse, make sure that you are running Eclipse
    with Admin rights**. Subclipse plugin will not work, if you install
    it to your Eclipse profile directory. It needs to be in Eclipse's
    global plugin directory. Especially if you are running Windows Vista
    or Windows 7, make sure that during the install you run Eclipse with
    "Run as administrator" and the plugin gets installed into glocal
    plugin directory. (If you run Eclipse normally, the plugin ends up
    to your personal Eclipse plugin directory.) Same advice for Linux.
    See here:
    <http://subclipse.tigris.org/ds/viewMessage.do?dsForumId=1047&dsMessageId=2625924>

<!-- end list -->

  -   
    Possible locations for
    org.tigris.subversion.subclipse.ui\_1.6.17.jar and other plugin
    components:
    :\* Wrong place:
    C:\\Users\\<username>\\.eclipse\\org.eclipse.platform\_3.6.1\_920333535\\plugins
    :\* Correct place: C:\\Program Files\\Eclipse\\plugins
    This is tricky. I have now twice been burned with the issue with
    Eclipse updates, and it always takes some time to figure it out...

<!-- end list -->

  - If you install Subclipse to a 64-bit Eclipse running with
    64-bit-java in Windows, you need to fetch an additional JavaHL
    library. See advice here: <http://subclipse.tigris.org/wiki/JavaHL>
    . You can get it e.g. from here:
    <http://www.sliksvn.com/en/download>

That way you can easily get the latest code.

### SVN Subversion 1.7

Please note that Subversion 1.7 has a different storage format for
working copies than the earlier 1.0-1.6 versions. An 1.