package game;

import gamedev.protocol.Protocol;
import gamedev.protocol.data.Claim;
import gamedev.protocol.data.River;

import java.util.*;

public class Intellect {

    public static final String GMANFACE = "#########%=+*::-.----..%##+-................-::--####\n" + "########@=+*:-.....  .. ... .............   ...@+####\n" + "########%+::--.....                         ...+=####\n" + "#######@=*:---... ..                         ..*#####\n" + "#######%=*:--......                         ...+@@###\n" + "#######%=::---......                         ..-@%###\n" + "#######%=*:::-.... ..    ..                  ..:@@###\n" + "#######%*+::----... ..                ....    .=@@###\n" + "#######@+*::----. .                      ......-#####\n" + "#######=+*-:-::-....                     .......%@###\n" + "#######=+*+:---.. ..          ..      ...-*--...*####\n" + "###+###=%%=*-@=+---......    .......-=####@%@#:.-@+##\n" + "####@####@#########@@+*-.. ..:-*%%#######@%#%@=.*#%##\n" + "####%####################@-.*@###########@=#@%*.@+@##\n" + "####@##############%@######:.*@#############@+..@:###\n" + "####@##@%@%############@##+.. +%@@########@%....+=###\n" + "#######@%*=@@##@##########%..-++*=#######+:-. . ++###\n" + "########=*:+%+::+===%#*+=+=...---.--@@=*-:-.....*=###\n" + "#####%###%+:-:+=##@*:-*++==.....-..........  . *.####\n" + "######%#%=++:-........:@==*.....:*-.        ..::.####\n" + "######%#%%#%%*-.......@@%=: . ...-::..     ..::+#####\n" + "########%%##@=-:.-..-@%@#+*. .---.-**.    ..*:#######\n" + "########%%####+=+*-+%%###%=...-::=..+#-. ..:*########\n" + "#########@####+#@#+#@==####%==@#%-....@. .-=.########\n" + "#############@+##@@#@%%@%@###@*+-......=--:*:########\n" + "#############@%%%####%=**=+@#*... .....:***++########\n" + "#############@##=#@###=+*::==-.*....:*%+:+*+#########\n" + "#############@##@#########+-:**%######@%:%:=#########\n" + "#################%#####@#####%+=.-.@=:*:+:-=#########\n" + "###########@#%####@#######@%*+%####=+=..:.@-#########\n" + "######@%##@#+*###=+@*########@#@%+*%*. .-#@:#########\n" + "#####@%#####:*+##%*=%-+=@=+......... . +@#:-#########\n" + "#####@%#####*+*+###@%%@=++...........-=@@-.-#########\n" + "####@@######=*:*+#####@#=@+:-*--*.-:*=@:..--#########\n" + "####@#######%=*---:#@@%###@%%%%@#%==:....---#########\n" + "#####%#####@@@=:--..-::*##@@%%=*--.......--*#########";


    public static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_CYAN = "\u001B[36m";

    private long summaryTimeOfThinking = 0l;
    private int moveCounter;
    private int timeOuts = 0;

    private State state;
    private Protocol protocol;

    //Здесь мы запоминаем сколько рек находится рядом с шахтой, чтобы
    //затем выбирать, возле каких из них нам покупать реку
    private Map<Integer, Integer> minesInfo = new HashMap();

    private Map<Integer, List<Integer>> minesVerticies = new HashMap();

    private Map<Integer, List<River>> citiesNeighbours = new HashMap<>();

    private Map<Integer, Map<Integer, Long>> calculatedSystems = new HashMap<>();
    private int maxCost = 0;

    private Deque<River> currentWay = new ArrayDeque<>();
    private int baseKey;

    private River lastMove;
    private int lastSystem = baseKey;

    private boolean routeTick = false;


    private Random randomizer;

    //Конструктор с инициализацией всех необходимых полей
    public Intellect(State state, Protocol protocol) {
        this.state = state;
        this.protocol = protocol;
        this.randomizer = new Random();
        init();
        printCitiesCosts();
        baseKey = (int) minesVerticies.keySet().iterator().next();
        System.out.println(ANSI_RED + "Проснитесь и пойте, Мистер Фримен, проснитесь и пойте..." + ANSI_RESET);
        showYourself();
    }

    private void init() {

        System.out.println("Заполняю информацию о шахтах...");
        for (Integer id : state.getMines()) {
            minesInfo.put(id, 0);
            minesVerticies.put(id, new ArrayList<Integer>());
            minesVerticies.get(id).add(id);
        }
        for (River river : state.getRivers().keySet()) {
            addToNeighbourMap(river);
            int source = river.component1();
            checkKey(source);
            int target = river.component2();
            checkKey(target);
        }
        calculateCityCosts(baseKey);

    }

    private void addToNeighbourMap(River river) {

        if (!citiesNeighbours.containsKey(river.getSource()))
            citiesNeighbours.put(river.getSource(), new LinkedList<River>());

        citiesNeighbours.get(river.getSource()).add(river);

        if (!citiesNeighbours.containsKey(river.getTarget())){}
            citiesNeighbours.put(river.getTarget(), new LinkedList<River>());

        citiesNeighbours.get(river.getTarget()).add(river);

    }

    //Запускает поиск в ширину от вершины и оценивает перспективность шахт (очень плохо)
    private void calculateCityCosts(Integer mine) {
        Set<Integer> visited = new HashSet<>();
        Queue<Integer> toVisit = new ArrayDeque<>();
        toVisit.add(mine);
        int count = 0;
        while (!toVisit.isEmpty()){
            int currentId = toVisit.poll();
            visited.add(currentId);
            calculatedSystems.put(mine, new HashMap<>());
            calculatedSystems.get(mine).put(currentId, (long) count);
            List<Integer> neighbours = getNeighbours(currentId);
            if (neighbours == null)
                continue;
            for (int neighbour: neighbours){
                if (!visited.contains(neighbour)) {
                    toVisit.add(neighbour);
                    visited.add(neighbour);
                }
            }
            count++;
        }
    }

    private List<Integer> getNeighbours(int currentId) {
        List<Integer> neighbours = new LinkedList<>();
        List<River> rivers = citiesNeighbours.get(currentId);
        if (rivers == null)
            return null;
        for (River river: rivers){
            if (river.getTarget() == currentId)
                neighbours.add(river.getSource());
            if (river.getSource() == currentId)
                neighbours.add(river.getTarget());
        }
        return neighbours;
    }

    private List<River> getNeighbourRivers(int id, boolean notEnemyOnly) {

        if (!notEnemyOnly)
            return citiesNeighbours.get(id);

        List<River> notEnemyRivers = new LinkedList<>();
        for (River river: citiesNeighbours.get(id)){
            if (state.getRivers().get(river) != RiverState.Enemy)
                notEnemyRivers.add(river);
        }

        return notEnemyRivers;
    }

    //Данный метод используется в fillMinesInfo() для проверки наличия
    //в таблице каждого отдельного ключа. Если ключ найден, значение,
    //соответствующее ему, увеличивается на единицу.
    private void checkKey(int key) {
        if (minesInfo.containsKey(key)) {
            minesInfo.replace(key, minesInfo.get(key) + 1);
        }
    }

    //Данный метод вызывается из класса Main после получения от сервера
    //сообщения о совершённом ходе. Мы обновляем наши знания о шахтах и
    //реках рядом с ними.
    public void update(Claim claim) {
        //System.out.println("Узнал о том, что кто-то забрал мою реку!");
        if (claim.getPunter() != state.getMyId()) {
            if (currentWay.contains(new River(claim.getSource(), claim.getTarget()))
                    || currentWay.contains(new River(claim.getTarget(), claim.getSource())))
                findAWayToClosestMine();
            if (minesInfo.containsKey(claim.getSource())) {
                decreaseMineValue(claim.getSource());
            }
            if (minesInfo.containsKey(claim.getTarget())) {
                decreaseMineValue(claim.getTarget());
            }
            //if (shouldSort != 0)
                //sortMinesInfo();
        }
    }

    public void timeout(){
        timeOuts++;
    }

    //Уменьшает значение в таблице по ключу и удаляет его, если оно стало
    //равно нулю
    private void decreaseMineValue(int key) {
        minesInfo.replace(key, minesInfo.get(key) - 1);
        if (minesInfo.get(key) <= 0)
            minesInfo.remove(key);
    }

    //Возвращает лист всех нейтральных рек
    private List<River> getNeutralRivers() {
        List<River> listOfRivers = new ArrayList<>();

        for (Map.Entry<River, RiverState> obj : state.getRivers().entrySet()) {
            if (obj.getValue() == RiverState.Neutral) {
                listOfRivers.add(obj.getKey());
            }
        }

        return listOfRivers;
    }



    private River tryToConnectMines(){
        if (minesVerticies.size() < 2)
            return null;

        River result;

        result = useCurrentWay();
        if (result != null)
            return result;

        findAWayToClosestMine();

        return useCurrentWay();
    }

    private River useCurrentWay() {

        if (currentWay.isEmpty())
            return null;

        River possibleResult;

        if (routeTick){
            possibleResult = currentWay.pollFirst();
            routeTick = false;
        } else {
            possibleResult = currentWay.pollLast();
            routeTick = true;
        }

        if (state.getRivers().get(possibleResult) != RiverState.Enemy){
            if (state.getRivers().get(possibleResult) == RiverState.Our){
                return useCurrentWay();
            } else
                return possibleResult;
        } else
            return null;
    }

    private void findAWayToClosestMine() {

        Integer startPoint;

        if (lastMove != null){
            if (checkIfOurCity(lastMove.getTarget()))
                startPoint = lastMove.getTarget();
            else
                startPoint = lastMove.getSource();
        } else
            startPoint = baseKey;

        //System.out.println("Пытаюсь найти кратчайший путь");

        if (!currentWay.isEmpty())
            currentWay.clear();

        HashMap<Integer, River> steps = new HashMap<>();
        Queue<Integer> toVisit = new ArrayDeque<>();
        steps.put(startPoint, null);
        toVisit.add(startPoint);
        int count = 0;
        while (!toVisit.isEmpty()){
            if (count > 5000)
                return;
            int currentId = toVisit.poll();
            if (checkIfDifferentSystem(currentId, startPoint)){
                rememberTheWay(currentId, steps);
                break;
            }
            checkId(currentId, steps, toVisit);
            count++;
        }

    }

    private void rememberTheWay(int id, HashMap<Integer, River> steps) {
        System.out.println("Запоминаю полученный список рек");
        int currentId = id;
        River currentRiver = steps.get(currentId);
        while (currentRiver != null) {
            currentWay.addFirst(currentRiver);
            currentId =
                    currentId == currentRiver.getSource() ?
                            currentRiver.getTarget() :
                            currentRiver.getSource();
            currentRiver = steps.get(currentId);
        }
    }

    private void checkId(int currentId, HashMap<Integer, River> steps, Queue<Integer> toVisit) {
        for (River neighbourRiver: getNotEnemyNeighbours(currentId)){
            int neighbourId =
                    currentId == neighbourRiver.getSource() ?
                            neighbourRiver.getTarget() :
                            neighbourRiver.getSource();
            if (!steps.containsKey(neighbourId)){
                toVisit.add(neighbourId);
                steps.put(neighbourId, neighbourRiver);
            }
        }
    }




    private boolean checkIfOurCity(int city) {
        for (List<Integer> system: minesVerticies.values()){
            for (Integer id: system){
                if (id == city)
                    return true;
            }
        }
        return false;
    }







    private List<River> getNotEnemyNeighbours(int currentId) {
        return getNeighbourRivers(currentId, true);
    }

    //Выбирает рандомную реку
    private River randomRiver() {
        Random random = new Random();
        List<River> neutralRivers = getNeutralRivers();
        River randomRiver = neutralRivers.get(random.nextInt(neutralRivers.size()));
        return randomRiver;
    }






    private River tryToMakeBadThings() {
        //System.out.println("Пытаюсь навредить!");
        River result = null;
        int lastPoints = -1;
        for (Map.Entry<River, RiverState> river : state.getRivers().entrySet()) {
            if (river.getValue() == RiverState.Neutral) {
                //System.out.println("Пытаюсь проверить этих парней: " + river.getKey().component1() + "-" + river.getKey().component2());
                byte nearestCode = checkIfMakingBad(river.getKey());
                //строка выше проверяет является ли река прилежащий
                //если код возврата 1 - значит река прилежит одной точкой
                //если код возврата 2 - значит река прилежит двумя точками
                if (nearestCode > 0){
                    int points = -2;
                    switch (nearestCode) {
                        case 1:
                            points = 10;
                            break;
                        case 2:
                            points = 15;
                            break;
                    }
                    if (points > lastPoints){
                        lastPoints = points;
                        result = river.getKey();
                    }
                }


            }
        }
        return result;
    }

    private byte checkIfMakingBad(River key) {
        byte result = 0;
        boolean sourceIsNear = false;
        boolean targetIsNear = false;
        for (Map.Entry<River, RiverState> river : state.getRivers().entrySet()) {
            if (river.getValue() == RiverState.Enemy) {
                if (checkIfRiversAreNear(key, river.getKey()) == 1)
                    sourceIsNear = true;
                if (checkIfRiversAreNear(key, river.getKey()) == 2)
                    targetIsNear = true;
            }
        }
        if (sourceIsNear)
            result++;
        if (targetIsNear)
            result++;
        return result;
    }





    //Метод возвращает первую попавшуюся нейтральную реку, прилежащую
    //любой УЖЕ принадлежащей нам
    private River chooseNearestRiver() {
        System.out.println("Пытаюсь найти ближайшего");
        River result = null;
        int lastPoints = -1;
        for (Map.Entry<River, RiverState> river : state.getRivers().entrySet()) {
            if (river.getValue() == RiverState.Neutral) {
                byte nearestCode = checkIfNearest(river.getKey());
                //строка выше проверяет является ли река прилежащий
                //если код возврата 1 - значит река прилежит одной точкой
                //если код возврата 2 - значит река прилежит двумя точками
                if (nearestCode > 0){

                    int points = -1;

                    Long citiesCostsPoints1 = calculatedSystems.get(lastSystem).get(river.getKey().getSource());
                    if (citiesCostsPoints1 != null){
                        if (citiesCostsPoints1 == 0)
                            points += maxCost + 10;
                        else
                            points += citiesCostsPoints1;
                    }

                    Long citiesCostsPoints2 = calculatedSystems.get(lastSystem).get(river.getKey().getTarget());
                    if (citiesCostsPoints2 != null){
                        if (citiesCostsPoints2 == 0)
                            points += maxCost + 10;
                        else
                            points += citiesCostsPoints2;
                    }


                    switch (nearestCode) {
                        case 1: {
                            //points *= 10;
                            break;
                        }
                        case 2: {
                            points -= maxCost * 10;
                            break;
                        }
                    }

                    if (checkIfRiversAreNear(river.getKey(), lastMove) > 0)
                        points *= 5;

                    if (checkIfCreatingConnection(river.getKey(), false) == -123456)
                        points = Integer.MAX_VALUE - 1;

                    if (points > lastPoints){
                        lastPoints = points;
                        result = river.getKey();
                    }
                }


            }
        }
        return result;
    }

    //Получает на вход нейтральную реку и сравнивает со всеми НАШИМИ
    //реками
    private byte checkIfNearest(River key) {
        byte result = 0;
        boolean sourceIsNear = false;
        boolean targetIsNear = false;
        for (Map.Entry<River, RiverState> river : state.getRivers().entrySet()) {
            if (river.getValue() == RiverState.Our) {
                    if (checkIfRiversAreNear(key, river.getKey()) == 1)
                        sourceIsNear = true;
                    if (checkIfRiversAreNear(key, river.getKey()) == 2)
                        targetIsNear = true;
            }
        }
        if (sourceIsNear)
            result++;
        if (targetIsNear)
            result++;
        return result;
    }

    //Проверяет прилежат ли реки друг к другу
    private byte checkIfRiversAreNear(River river1, River river2) {
        byte result = 0;

        if (river1.getSource() == river2.component1() ||
                river1.getSource() == river2.component2()) {
            result = 1;
        }
        if (river1.getTarget() == river2.component1() ||
                river1.getTarget() == river2.component2()) {
            result = 2;
        }

        return result;
    }





    //Выбор реки исходя из наших знаний о шахтах
    private River claimFromMineInfo() {
        System.out.println("У меня же есть список шахт!");
        int mineId = minesInfo.entrySet().iterator().next().getKey();
        minesInfo.remove(mineId);

        return findRiver(mineId);
    }




    //Найти реку по ID
    private River findRiver(int mineId) {
        River result = null;
        for (Map.Entry<River, RiverState> river : state.getRivers().entrySet()) {
            if (river.getValue() == RiverState.Neutral) {
                if (river.getKey().component1() == mineId || river.getKey().component2() == mineId){
                    result = river.getKey();
                    return result;
                }
            }
        }
        return result;
    }





    //Делает ход
    public void makeMove() {

        long start = System.currentTimeMillis();

        if (!currentWay.isEmpty())
            printCurrentWay();

        River choice = chooseRiver();

        if (choice == null)
            protocol.passMove();
        else {
            int newSystem = checkIfCreatingConnection(choice, true);
            lastMove = choice;
            protocol.claimMove(choice.getSource(), choice.getTarget());
            if (newSystem > 0 && newSystem != lastSystem){

                if (!calculatedSystems.containsKey(newSystem)){
                    System.out.println("Пересчитываю цены для новой системки");
                    calculatedSystems.put(newSystem, new HashMap<>());
                    //printCitiesCosts();
                    calculateCityCosts(newSystem);
                }

                lastSystem = newSystem;
            }
        }

        long finish = System.currentTimeMillis();

        checkTheStatisticsBro(finish - start);
        return;

    }

    private void checkTheStatisticsBro(long l) {
        summaryTimeOfThinking += l;
        moveCounter++;
        System.out.println(ANSI_YELLOW + "Номер хода: " + moveCounter + ANSI_RESET);
        System.out.println(ANSI_YELLOW + "Время хода: " + l + "мс" + ANSI_RESET);
        System.out.println(ANSI_YELLOW + "MidTime: " + summaryTimeOfThinking/moveCounter + "мс" + ANSI_RESET);
        System.out.println(ANSI_YELLOW + "Всего я думал " + summaryTimeOfThinking/1000 + " секунд" + ANSI_RESET);
        if (timeOuts != 0){
            System.out.println(ANSI_RED + "TIMEOUTS: " + timeOuts + ANSI_RESET);
        }
        return;
    }

    //Метод с основной логикой выбора реки
    private River chooseRiver() {
        River choice;

        if (!minesInfo.isEmpty()) {
            choice = claimFromMineInfo();
        } else {
            choice = tryToConnectMines();
            if (choice == null){
                choice = chooseNearestRiver();
                if (choice == null){
                    choice = tryToMakeBadThings();
                    if (choice == null)
                        choice = randomRiver();
                }
            }
        }

        return choice;
    }




    //Если река соединяет две разные системы - возвращает "-123456"
    //Если ни одна точка не принадлежит нашей системе, возвращает "-1"
    //Если же только одна из точек принадлежит системе, вовзращается индекс системы
    private int checkIfCreatingConnection(River choice, boolean shouldChangeInfo) {
        int target = choice.getTarget();
        int source = choice.getSource();

        Integer targetKey = null;
        Integer sourceKey = null;
        for (Map.Entry<Integer, List<Integer>> obj: minesVerticies.entrySet()){
            for (Integer id: obj.getValue()){
                if (id == target)
                    targetKey = obj.getKey();
                if (id == source)
                    sourceKey = obj.getKey();
                if (sourceKey != null && targetKey != null)
                    break;
            }
        }

        if (targetKey == null && sourceKey == null){
            return -1;
        }



        if (targetKey != null && sourceKey == null){
            if (shouldChangeInfo)
                minesVerticies.get(targetKey).add(source);
            return targetKey;
        }

        if (targetKey == null){
            if (shouldChangeInfo)
                minesVerticies.get(sourceKey).add(target);
            return sourceKey;
        }

        if (shouldChangeInfo){
            minesVerticies.get(targetKey).addAll(minesVerticies.get(sourceKey));
            minesVerticies.remove(sourceKey);
        }

        return -123456;

    }

    private boolean checkIfDifferentSystem(int source, int target){
        Integer targetKey = null;
        Integer sourceKey = null;
        for (Map.Entry<Integer, List<Integer>> obj: minesVerticies.entrySet()){
            for (Integer id: obj.getValue()){
                if (id == target)
                    targetKey = obj.getKey();
                if (id == source)
                    sourceKey = obj.getKey();
                if (sourceKey != null && targetKey != null)
                    break;
            }
        }

        if (targetKey == sourceKey)
            return false;
        else {
            if (targetKey != null && sourceKey != null)
                return true;
            else
                return false;
        }

    }




    private void printCitiesCosts() {
        System.out.println("*******************");
        for (Map.Entry<Integer, Long> city: calculatedSystems.get(lastSystem).entrySet()){
            System.out.println(city.getKey() + ": " + city.getValue());
        }
        System.out.println("*******************");
    }

    private void printCurrentWay(){
        System.out.println("№№№№№№№№№№№№№№№");
        int count = 0;
        for (River river: currentWay){
            count++;
            System.out.println(count + ": " + river.getSource() + "-" + river.getTarget());
        }
        System.out.println("№№№№№№№№№№№№№№№");
    }


    private void showYourself() {
        System.out.println(ANSI_CYAN + GMANFACE + ANSI_RESET);
        return;
    }
}
