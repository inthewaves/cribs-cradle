# CRADLE5 trial app

## Development

### Building

Inside of `local.properties`, fill in these properties:

```properties
cradle5AppKeystorePassword=
cradle5AppKeyAlias=
cradle5AppKeyPassword=
```

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