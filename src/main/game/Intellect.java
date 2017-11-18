package game;

import gamedev.protocol.Protocol;
import gamedev.protocol.data.Claim;
import gamedev.protocol.data.River;

import java.util.*;
import java.util.stream.Collectors;

public class Intellect {

    private State state;
    private Protocol protocol;

    //Здесь мы запоминаем сколько рек находится рядом с шахтой, чтобы
    //затем выбирать, возле каких из них нам покупать реку
    private Map<Integer, Integer> minesInfo = new HashMap();

    private Map<Integer, List<Integer>> minesVerticies = new HashMap();

    private Map<Integer, Long> citiesCosts = new HashMap();

    private River lastMove;

    private Deque<River> currentWay = new ArrayDeque<>();

    private Random randomizer;

    public Intellect(State state, Protocol protocol) {
        this.state = state;
        this.protocol = protocol;
        this.randomizer = new Random();
        fillMinesInfo();
        fillCitiesCosts();
        printCitiesCosts();
        System.out.println("Инициализируюсь...");
    }

    private void fillCitiesCosts() {
        for (Integer mine: state.getMines()){
            calculateCityCosts(mine);
        }
    }

    private void calculateCityCosts(Integer mine) {
        Set<Integer> visited = new HashSet<>();
        Queue<Integer> toVisit = new ArrayDeque<>();
        toVisit.add(mine);
        long count = 0L;
        while (!toVisit.isEmpty()){
            int currentId = toVisit.poll();
            visited.add(currentId);
            if (citiesCosts.containsKey(currentId))
                citiesCosts.replace(currentId, citiesCosts.get(currentId) + count*count);
            else
                citiesCosts.put(currentId, count*count);
            for (int neighbour: getNeighbours(currentId, visited)){
                toVisit.add(neighbour);
            }
            count++;
        }
    }

    private List<Integer> getNeighbours(int currentId, Set<Integer> visited) {
        List<Integer> neighbours = new LinkedList<>();
        for (River river: state.getRivers().keySet()){
            if (river.getSource() == currentId && !visited.contains(river.getTarget())){
                neighbours.add(river.getTarget());
            }
            if (river.getTarget() == currentId && !visited.contains(river.getSource())){
                neighbours.add(river.getSource());
            }
        }
        return neighbours;
    }

    //Здесь мы инициализируем нашу таблицу с шахтами, проходясь
    //по всем рекам и считая, сколько рек находится возле каждой
    //шахты.
    private void fillMinesInfo() {
        System.out.println("Заполняю информацию о шахтах...");
        for (Integer id : state.getMines()) {
            minesInfo.put(id, 0);
            minesVerticies.put(id, new ArrayList<Integer>());
            minesVerticies.get(id).add(id);
        }
        for (River river : state.getRivers().keySet()) {
            int source = river.component1();
            checkKey(source);
            int target = river.component2();
            checkKey(target);
        }
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

        int idOfSource = getRandomNetDot();
        findAWayToClosestMine(idOfSource);

        return useCurrentWay();
    }

    private River useCurrentWay() {
        if (currentWay.isEmpty())
            return null;
        River possibleResult = currentWay.getFirst();
        if (state.getRivers().get(possibleResult) == RiverState.Neutral)
            return possibleResult;
        return null;
    }



    private void findAWayToClosestMine(int mine) {
        if (!currentWay.isEmpty())
            currentWay.clear();
        HashMap<Integer, River> steps = new HashMap<>();
        Queue<Integer> toVisit = new ArrayDeque<>();
        steps.put(mine, null);
        toVisit.add(mine);
        while (!toVisit.isEmpty()){
            int currentId = toVisit.poll();
            if (state.getMines().contains(currentId) && checkIfDifferentSystem(currentId, mine)){
                rememberTheWay(currentId, steps);
                break;
            }
            checkId(currentId, steps, toVisit);
        }

    }

    private void rememberTheWay(int id, HashMap<Integer, River> steps) {
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
        for (River neighbourRiver: getNeutralNeighbours(currentId)){
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

    private List<River> getNeutralNeighbours(int currentId) {
        List<River> neighbours = new LinkedList<>();
        for (River river: state.getRivers().keySet()){
            if (state.getRivers().get(river) != RiverState.Neutral)
                continue;
            if (river.getSource() == currentId || river.getTarget() == currentId)
                neighbours.add(river);
        }
        return neighbours;
    }

    private int getRandomNetDot() {
        return minesVerticies.entrySet().iterator().next().getKey();
    }

    //Выбирает рандомную реку
    private River randomRiver() {
        Random random = new Random();
        List<River> neutralRivers = getNeutralRivers();
        River randomRiver = neutralRivers.get(random.nextInt(neutralRivers.size()));
        return randomRiver;
    }

    //Метод с основной логикой выбора реки
    private River chooseRiver() {

        //System.out.println("Пытаюсь выбрать реку...");

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
        River result = null;
        long lastPoints = -1;
        for (Map.Entry<River, RiverState> river : state.getRivers().entrySet()) {
            if (river.getValue() == RiverState.Neutral) {
                byte nearestCode = checkIfNearest(river.getKey());
                //строка выше проверяет является ли река прилежащий
                //если код возврата 1 - значит река прилежит одной точкой
                //если код возврата 2 - значит река прилежит двумя точками
                if (nearestCode > 0){

                    long points = -2;

                    Long citiesCostsPoints1 = citiesCosts.get(river.getKey().getSource());
                    if (citiesCostsPoints1 != null)
                        points += citiesCostsPoints1;

                    Long citiesCostsPoints2 = citiesCosts.get(river.getKey().getTarget());
                    if (citiesCostsPoints2 != null)
                        points += citiesCostsPoints2;

                    switch (nearestCode) {
                        case 1: {
                            points *= 5;
                            break;
                        }
                        case 2: {
                            break;
                        }
                    }


                    if (checkIfCreatingConnection(river.getKey(), false))
                        points += 1000000;
                    if (checkIfRiversAreNear(river.getKey(), lastMove) > 0)
                        points *= 2;
                    if (points > lastPoints){
                        lastPoints = points;
                        result = river.getKey();
                    }
                }


            }
        }
        return result;
    }

    private int checkPoints(int source) {
        int points = 100;
        /*Set ourRivers = state.getRivers()
                                .entrySet()
                                .stream()
                                .filter(p -> p.getValue() == RiverState.Our)
                                .collect(Collectors.toSet());
        ourRivers.
        */
        return points;
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
        //minesInfo.remove(mineId);
        decreaseMineValue(mineId);

        //ОПАСНО! Пытаюсь ДВАЖДЫ уменьшить переменную с количеством рек возле
        //шахты, чтобы бот перестал туда ходить
        if (minesInfo.containsKey(mineId))
            decreaseMineValue(mineId);
        if (minesInfo.containsKey(mineId))
            decreaseMineValue(mineId);



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
        //printMineInfo();
        //printMineVerticies();
        River choice = chooseRiver();
        if (choice == null)
            protocol.passMove();
        else {
            lastMove = choice;
            checkIfCreatingConnection(choice, true);
            //System.out.println("Покупаю реку " + choice.getTarget() + "-" + choice.getSource());
            protocol.claimMove(choice.getSource(), choice.getTarget());
        }

        //System.out.println("Я походил. Не бей по лицу.");
    }

    private boolean checkIfCreatingConnection(River choice, boolean shouldChangeInfo) {
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
            if (sourceKey != null && targetKey != null)
                break;
        }

        if (targetKey == sourceKey)
            return false;

        if (targetKey != null && sourceKey == null){
            if (shouldChangeInfo)
                minesVerticies.get(targetKey).add(source);
            return false;
        }

        if (targetKey == null){
            if (shouldChangeInfo)
                minesVerticies.get(sourceKey).add(target);
            return false;
        }

        if (shouldChangeInfo){
            minesVerticies.get(targetKey).addAll(minesVerticies.get(sourceKey));
            minesVerticies.remove(sourceKey);
        }
        return true;
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
            if (sourceKey != null && targetKey != null)
                break;
        }

        if (targetKey == sourceKey)
            return false;
        else
            return true;
    }

    private void printCitiesCosts() {
        System.out.println("*******************");
        for (Map.Entry<Integer, Long> city: citiesCosts.entrySet()){
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

    private void printMineInfo() {
        System.out.println("-----");
        for (Map.Entry<Integer, Integer> mine: minesInfo.entrySet()){
            System.out.println("Шахта: " + mine.getKey() + ", Рек: " + mine.getValue());
        }
        System.out.println("-----");
    }

    private void printMineVerticies() {
        System.out.println("+++++");
        for (Map.Entry<Integer, List<Integer>> obj: minesVerticies.entrySet()){
            System.out.print("Шахта: " + obj.getKey() + ", Города:");
            for (Integer integer: obj.getValue()){
                System.out.print(" " + integer);
            }
            System.out.println();
        }
        System.out.println("+++++ ");
    }


}
