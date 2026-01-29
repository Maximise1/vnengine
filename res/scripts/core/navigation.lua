function show_main_menu()
    show_screen("main_menu", {})
end

function new_game()
    print("Starting new game...")
    show_screen("game_screen", {})
    start_game()
    -- Call your GameEngine.runGame() here
end

function load_game()
    print("Loading game...")
end

function options()
    print("Opening options...")
end

function exit()
    print("Exiting...")
end