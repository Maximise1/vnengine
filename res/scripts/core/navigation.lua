function show_main_menu()
    show_screen("main_menu", {})
end

function new_game()
    show_screen("game_screen", {})
    start_game()
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