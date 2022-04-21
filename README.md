# CRIBS CRADLE (Cradle 5)

An app developed specifically for the Cradle5 trial.

## Development

This is an Android app written 100% in Kotlin using Jetpack Compose (UI). This was both rushed and
also my first time working with Compose.

Some code here has been heavily inspired by official Compose samples. For example,

* The navigation code was adapted from the [Tivi](https://github.com/chrisbanes/tivi) app (although 
  I still don't agree with the androidx.navigation approach of using URL-based navigation without 
  type-safe compiler-checked navigation args)
* Much of the Compose-related form code is derived from 
  [JetSurvey](https://github.com/android/compose-samples/tree/master/Jetsurvey)
  
### Structure

There are several modules to this project.

* **util**: A module containing utilities used by all other modules
* **api**: Strictly related to serializing / deserializing JSON from the MedSciNet API
* **data**: Contains information on how the data is stored in the app (using Room on SQLite + 
  SQLCipher, but see [issue 8](https://github.com/inthewaves/cribs-cradle/issues/8) on whether
  SQLCipher is unneeded technical debt)
* **domain**: Connects the **api** and **data** modules together
* **app**: Contains the main Android app code (Compose UI, ViewModels, etc.)

### Building

Inside of `local.properties`, fill in these properties:

```properties
cradle5AppKeystorePassword=
cradle5AppKeyAlias=
cradle5AppKeyPassword=
```

The encrypted release keystore has been checked into the Git repository.

### Debugging stack traces

Based on https://developer.android.com/studio/command-line/retrace

R8 is used to shrink the code, because Jetpack Compose libraries are very large without code
shrinking. This results in raw stack traces from crashes becoming unreadable or inaccurate.

To get the exact stacktrace, ensure you first save the mapping from 
`app/build/outputs/mapping/{buildType}/mapping.txt` with every single build that you release (Google
Play Console will ask for this `mapping.txt` when uploading an app bundle). 

Download the command tools from https://developer.android.com/studio#command-tools to get the 
`retrace` program. Then use the retrace program to map the obfuscated stacktrace to the original 
stacktrace. Using doing the following will work:

```bash
retrace app/build/outputs/mapping/staging/mapping.txt
```

After the above command is run, the program awaits input from stdin. The stacktrace (can be directly
from `adb logcat` or an Android bug report) can now be pasted, and then finally you need to 
terminate the input stream (<kbd>Ctrl</kbd> + <kbd>D</kbd> on Linux / macOS; <kbd>Ctrl</kbd> + 
<kbd>Z</kbd> + <kbd>Enter</kbd> on Windows). Alternatively, `retrace` accepts a text file containing
the stack trace as the second argument

When viewing crash stack traces in Play Console, they handle the `retrace` step for us. This is more
for development builds.

## License

Copyright 2021 Paul Ngo

Licensed under GPLv3: https://www.gnu.org/licenses/gpl-3.0.html
