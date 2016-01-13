Battlecode Project Scaffold
===========================

Here you'll find (almost) everything you need to write players for Battlecode
2016.

Other documentation and resources can be found at: https://www.battlecode.org/


## Overview

### Project structure

- `README.md`
    This file.
- `build.xml`
    The Ant build file used to build and run players.
- `ivy.xml`
    The Ivy file used to find and download dependencies
- `bc.conf`
    The battlecode configuration file containing user settings.
- `src/`
    Player source code
- `test/`
    Player test code
- `lib/`
    Dependencies directory
- `bin/`
    The output directory for builds; can be safely ignored
- `.ivy/`
    An extra directory containing ivy resources; can be safely ignored


### How does Battlecode work?

The Battlecode software consists of three major components:

- The player library/API: these are the classes that you will import and build
  against when writing a player.

- The server: this is the software that computes Battlecode matches. For most
  users, the server will run transparently, so you don't have to worry about it.
  However, advanced server setups are possible, allowing you to compute matches
  on one machine and view them on another.
    
- The client: this is the software that displays Battlecode matches. For most
  users, the client will automatically create a server for running a match and
  display that match as it computes. The client also plays match files like
  those from scrimmage matches and the tournaments.

This project scaffold handles installing and running these components using Ant
and Ivy.


### What is Ant?

Apache Ant is a Java-based build system similar in theory to UNIX `make`.

You can run it from a terminal or from an IDE; instructions are below.

You can find Ant's documentation at: http://ant.apache.org/

You are not required to use the Ant build script, but you should probably at
least read it to get an idea of how things work.


### What is Ivy?

Apache Ivy is a "dependency manager" that integrates well with Ant. It handles
finding and downloading "dependencies": resources that a project needs to build
and run.

The Ant build file handles loading and invoking Ivy; you shouldn't have to
concern yourself with it, unless you want to add more dependencies for some
reason.

You can find Ivy's documentation at: http://ant.apache.org/ivy/


## Getting started

First, you'll need a Java Development Kit compatible with Java 8 or later.

You can find JDK installers at:
http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html

Alternatively, you can install a JDK yourself using your favorite package
manager. Make sure it's an Oracle JDK - we don't support anything else -
and is compatible with Java 8.

Next, you'll need to choose how you want to work on battlecode - using an
IDE, using a terminal, or mixing and matching.


### Using Eclipse

- Install and open the latest version of Eclipse:
  http://www.eclipse.org/downloads/packages/eclipse-ide-java-developers/mars1

- Create a new Eclipse workspace. The workspace should NOT contain the
  battlecode-scaffold folder.

- Run `File -> Import...`, and select `General / Existing Projects Into
  Workspace`

- In the `Select root directory` field, navigate to `battlecode-scaffold/ide/eclipse`.
  Note: DO NOT select just `battlecode-scaffold` as the root directory; you have
  to select `battlecode-scaffold/ide/eclipse`.
  Finish importing the project.

- Click the dropdown arrow next to the `External Tools` icon (a play button with
  a toolbox). You should see a set of tasks our build file can run.

- Run `Update Battlecode`.

- You're good to go; you can run other ant tasks using the other `External Tools`
  targets.


### Using IntelliJ IDEA
- Install IntelliJ IDEA Community Edition:
  https://www.jetbrains.com/idea/download/

- In the `Welcome to IntelliJ IDEA` window that pops up when you start IntelliJ,
  select `Open` (NOT `Import project` or `Create new project`)

- In the `Open File or Project` window, select `battlecode-scaffold/ide/intellij`
  (NOT just `battlecode-scaffold`).

- Select the `Update Battlecode` ant target in the run configurations box (a
  rounded rectangle next to a green triangle) and run it (using the green
  triangle). You can also run it using the `Run / Run...` menu option.

- You're good to go; you can run other ant tasks using the other run
  configurations, or using the `Ant Build` tool window (accessible from
  `View / Tool Windows / Ant Build`)


### Using a terminal

- Install Apache Ant, the build tool used in the project. You can do it
  manually: http://ant.apache.org/manual/install.html#getting
  Or, you can use your favorite package manager.

  On every system you will need to set the JAVA_HOME environment variable to
  point to the installation path of your JDK.

  You may also need the ANT_HOME environment variable in some cases. Just set
  this to be the path to your ant installation and you should be good to go.

- Navigate to the root directory of the project, and run `ant update`.

- You're good to go. Run `ant -p` to see the other ant build
  tasks available.


## Writing Players

The included `build.xml` file allows you to compile your player and prepare it
for submission without having to worry about the Java classpath and other
settings. To take advantage of this, simply place the source code for your
player(s) in a subdirectory of `src` folder in your Battlecode installation
directory.

For instance, if you are team #1, you'd have a folder called `team001` in your
`src` folder, and in `team001` you'd have `RobotPlayer.java` (with package
`team001`), etc.

You can also have other folders, if you want to work on multiple players at once.
However, only the `teamXXX` player can be submitted for your team.

Building after running this might help resolve issues with version changes.

## Running Matches

### Local

Local matches are the most common way to run a match - they are computed and
rendered simultaneously on the same machine. Invoke the ant `run` target to run
a local match, using the command line or an IDE. 

A dialog box will appear that allows you to choose the teams and maps to run.
The teams and maps that appear in the dropdown boxes are those that the software
knows about at runtime. If the team or map you're trying to run does not appear
in the dropdown box, it isn't on your classpath or map path.

When running a local match, you also have the option of saving the match to a
file so that you can play it back without having to recompute it. To do this,
check the "Save match to file" box on the main dialog and choose the location of
the file. Note that the file will be overwritten if it already exists.

If you're not using Ant, you can run the `battlecode.client.Main` class from the
command line or an IDE. You should pass an argument `-c [CONF_FILE]` to point it
at a battlecode configuration file.

### Headless matches

Headless matches run a match without viewing it - they run the server without
the client. Invoke the ant `headless` command to run a headless match.

You can edit `bc.conf` to change the players and maps run during a headless
match, or supply ant with a `-Dbc.conf=[CONF_FILE]` argument to use
another battlecode configuration file.

If you're not using Ant, you can run the `battlecode.server.Main` class from the
command line or an IDE. You should pass an argument `-c [CONF_FILE]` to point it
at a battlecode configuration file.


### Playing Back from a File

If you have a match file that you'd like to play back (i.e., from saving one
previously) you can choose "Play back from match file" and choose the match file
to play back. The remainder of the dialog settings will be ignored.


### Match Sets

This year, each match between two teams consists of a set of games. To run
multiple games in a match, use the add and remove buttons below the map dropdown
box to add maps to the list. A game will be played on each map in the list, in
order. If you don't add any maps, the match will consist of one game, on the map
selected in the dropdown box.


## Debugging your Player

Normally, the software computes the match well ahead of what is being currently
viewed. However, selecting "compute and view match synchronously" from the match
dialog puts the software in "lockstep mode", where the computation and viewing
move in lockstep. This is generally slower than running them independently, but
it allows for some interesting debugging features.

While in lockstep mode, right-clicking on an open square in the map brings up a
menu that lets you add new units to the map. Right-clicking on an existing unit
allows you to set its control bits, which the robot's player can query and react
to. You can also drag-and-drop units on the map.

These debugging features are intended to help you test your robots in situations
that might otherwise be hard to get them into (e.g., what happens if one of my
archons gets cut off from the rest...?). However, if the players are not written
defensively, these unexpected manual changes can interfere with their control
logic. Keep this in mind when using the debugging features.

Also, during the tournament and scrimmages, you will not be able to manually
affect the game in this way.


## Uploading your Player

You should upload a zip or jar file containing your team's source code.

You should use the package name `teamXXX` where XXX is your team number.

You can build this jar automatically using the command

```
ant -Dteam=teamXXX jar
```

Alternatively, creating a zip file of the `src/teamXXX` directory should work.


## Advanced Configuration

The Battlecode distribution includes a configuration file, `bc.conf`, that
allows you to tweak some of the software's settings. Appendix A contains a
listing of configurable properties.

The properties can also be set in any way that Java properties are set. This
includes using `-D[property]=[value]` on the `java` or `ant` command line.


## Appendix A: Configuration Properties and Command-line Arguments

### Computation Throttling

When running a local match, the game engine attempts to periodically delay to
prevent starving the match viewer of CPU time. Altering the following two
settings may yield better local match performance:

- `bc.server.throttle`: determines how to delay match computation; can be set to
   either "yield" or "sleep"
- `bc.server.throttle-count`: the number of rounds between sleep/yield


### Engine Settings

The following settings can be used to enable or disable certain aspects of the
engine.

- `bc.engine.silence-a` and `bc.engine.silence-b`: "true" or "false"; whether or
   not the engine will suppress printouts for the appropriate team
- `bc.engine.gc`: "true" or "false"; whether or not to periodically force
   garbage collection in the game engine -- this option causes decreased
   performance but may help if the virtual machine runs out of memory during
   computation
- `bc.engine.gc-rounds`: how many rounds between forced invocation of the
   garbage collector; the default is 50
- `bc.engine.upkeep`: "true" or "false"; if "false", the engine will not charge
   units their energon upkeep each round
- `bc.engine.breakpoints`: "true" or "false"; if "false", the engine will skip
   breakpoints in player code


### Client Settings
- `bc.game.renderprefs2d`: preferences for the 2d client.  For example, if
   you wanted to turn off the rendering of broadcasts and gridlines, you could
   set this property to "bg".  See the "Shortcut Keys" section for a complete
   listing of toggles.


### Miscellaneous Settings

- `bc.game.map-path`: the folder in which to look for map files (aside from the
   ones packaged into the battlecode software)
- `bc.dialog.skip`: "true" or "false"; whether or not to show the setup dialog
   when the software is started. If "true", the parameters most recently entered
   into the dialog will be used.


### Client Settings

- `bc.client.renderprefs2d`: A list of toggles to set in the 2D client.
   (See Shortcut Keys below.)  For example, if you want to turn off broadcasts,
   grid lines, and transfers, you would set `bc.client.renderprefs2d=bgt`.
- `bc.client.sound-on`: "true" or "false"; whether or not to play sound
   effects.


### Shortcut Keys

| Key | Effect 
|-----|--------
|  B  | Toggle unit broadcasts
|  D  | Toggle discrete movement mode
|  E  | Toggle HP bars
|  F  | Toggle fast forward
|  G  | Toggle grid lines
|  H  | Toggle action lines
|  I  | Rewind 50 rounds
|  J  | Toggle slow mo
|  L  | Toggle low supply indicators
|  K  | Toggle attack lines
|  R  | Show attack/sight ranges when examining a unit
|  S  | Skip 100 rounds
|  V  | Toggle indicator dot/line display (none, one team, both teams)
|  X  | Toggle unit explosions
|  /  | Find unit by ID
|  <  | Pause
|  >  | Pause
| Esc | Quit


## Scala

Most contestants choose to write their players in Java, but we also support
Scala (or a mix of Java and Scala).  If you want to use Scala, simply add a
scala file to any of your players, and re-run `ant update`. Everything you
need should now be installed.
