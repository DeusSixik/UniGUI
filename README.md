# UniGUI
UniGUI is a UI framework built on top of my library for the Unigine engine. It is designed not to be directly tied to 
the Minecraft renderer, making it portable without requiring a complete core rewrite. <br>

The main goal of this library is to deliver maximum UI performance and follow the "write once, run anywhere" principle. <br> <br>
We use a dedicated DrawCommands system that processes tasks and allows them to be grouped into batch buckets for maximum performance. 
Since the library is fully integrated into Minecraft, there should be no context issues.