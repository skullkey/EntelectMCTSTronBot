rm game.txt
while [ 1 ]
do
  java -jar mctsbot.jar entelectmap/gamestate crossover a b > game.txt
  clear
  cat game.txt
  java -jar mctsbot.jar entelectmap/gamestate crossover b a > game.txt
  clear
  cat game.txt
done
