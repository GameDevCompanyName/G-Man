package game;

import gamedev.protocol.Protocol;
import gamedev.protocol.data.Claim;
import gamedev.protocol.data.River;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class Intellect {

    private State state;
    private Protocol protocol;

    //Здесь мы запоминаем сколько рек находится рядом с шахтой, чтобы
    //затем выбирать, возле каких из них нам покупать реку
    private Map<Integer, Integer> minesInfo = new HashMap();
    boolean init = false;

    public Intellect(State state, Protocol protocol) {
        this.state = state;
        this.protocol = protocol;
        System.out.println("Инициализируюсь...");
    }

    //Здесь мы инициализируем нашу таблицу с шахтами, проходясь
    //по всем рекам и считая, сколько рек находится возле каждой
    //шахты.
    private void fillMinesInfo() {
        System.out.println("Заполняю информацию о шахтах...");
        for (Integer id : state.getMines()) {
            minesInfo.put(id, 0);
        }
        for (River river : state.getRivers().keySet()) {
            int source = river.component1();
            checkKey(source);
            int target = river.component2();
            checkKey(target);
        }
        //Следующий кусок копипасты должен отсортировать нашу HashMap
        //God bless StackOverflow
        sortMinesInfo();
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
        System.out.println("Узнал о том, что кто-то забрал мою реку!");
        if (claim.getPunter() != state.getMyId()) {
            byte shouldSort = 0;
            if (minesInfo.containsKey(claim.getSource())) {
                decreaseMineValue(claim.getSource());
                shouldSort++;
            }
            if (minesInfo.containsKey(claim.getTarget())) {
                decreaseMineValue(claim.getTarget());
                shouldSort++;
            }
            if (shouldSort != 0)
                sortMinesInfo();
        }
    }

    //Сортировка нашей minesInfo после её изменения в update()
    private void sortMinesInfo() {
        System.out.println("Навожу порядок в списке шахт");
        minesInfo.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        HashMap::new
                ));
    }

    //Уменьшает значение в таблице по ключу и удаляет его, если оно стало
    //равно нулю
    private void decreaseMineValue(int key) {
        minesInfo.replace(key, minesInfo.get(key) - 1);
        if (minesInfo.get(key) == 0)
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

    //Выбирает рандомную реку
    private River randomRiver() {



        Random random = new Random();
        List<River> neutralRivers = getNeutralRivers();
        River randomRiver = neutralRivers.get(random.nextInt(neutralRivers.size()));
        System.out.println("Я не знаю, что ещё остаётся: беру реку " + randomRiver.component1() + "-" + randomRiver.component2());
        return randomRiver;
    }

    //Метод с основной логикой выбора реки
    private River chooseRiver() {

        System.out.println("Пытаюсь выбрать реку...");

        if (!minesInfo.isEmpty()) {
            return claimFromMineInfo();
        } else {
            return chooseNearestRiver();
        }
    }

    //Метод возвращает первую попавшуюся нейтральную реку, прилежащую
    //любой УЖЕ принадлежащей нам
    private River chooseNearestRiver() {
        River result = null;
        for (Map.Entry<River, RiverState> river : state.getRivers().entrySet()) {
            System.out.println("Пытаюсь найти ближайшую");
            if (river.getValue() == RiverState.Neutral) {
                System.out.println("Пытаюсь проверить этих парней:" + river.getKey().component1() + "-" + river.getKey().component2());
                if (checkIfNearest(river.getKey())){
                    result = river.getKey();
                    return result;
                }

            }
        }
        return result;
    }

    //Получает на вход нейтральную реку и сравнивает со всеми НАШИМИ
    //реками
    private boolean checkIfNearest(River key) {
        System.out.println("Перебираю все наши реки, пытясь найти лежащую рядом");
        for (Map.Entry<River, RiverState> river : state.getRivers().entrySet()) {
            if (river.getValue() == RiverState.Our) {
                if (checkIfRiversAreNear(key, river.getKey()))
                    return true;
            }
        }
        System.out.println("Не нашёл лежащей рядом!");
        return false;
    }

    //Проверяет прилежат ли реки друг к другу
    private boolean checkIfRiversAreNear(River river1, River river2) {
        if (river1.component1() == river2.component1() ||
                river1.component1() == river2.component2() ||
                river1.component2() == river2.component1() ||
                river1.component2() == river2.component2()) {
            System.out.println("Нашёл лежащую рядом!");
            return true;
        }
        return false;
    }

    //Выбор реки исходя из наших знаний о шахтах
    private River claimFromMineInfo() {
        System.out.println("У меня же есть список шахт!");
        int mineId = minesInfo.entrySet().iterator().next().getKey();
        minesInfo.remove(mineId);
        //decreaseMineValue(mineId);
        return findRiver(mineId);
    }

    //Найти реку по ID
    private River findRiver(int mineId) {
        System.out.println("Попробую поискать реку возле шахты " + mineId);
        River result = null;
        for (Map.Entry<River, RiverState> river : state.getRivers().entrySet()) {
            System.out.println("Пытаюсь проверить эту реку на нейтральность:" + river.getKey().component1() + "-" + river.getKey().component2());
            if (river.getValue() == RiverState.Neutral) {
                System.out.println("Пытаюсь проверить этих парней:" + river.getKey().component1() + "-" + river.getKey().component2());
                if (river.getKey().component1() == mineId || river.getKey().component2() == mineId){
                    result = river.getKey();
                    System.out.println("Нашёл ключ! + " + river.getKey().component1() + "-" + river.getKey().component2());
                    return result;
                }
                System.out.println("Пытаюсь найти реку, сейчас: " + result);
            }
        }
        return result;
    }

    //Делает ход
    public void makeMove() {
        //River choice = randomRiver(state.getRivers());
        if (!init){
            init = true;
            fillMinesInfo();
        }
        printMineInfo();
        River choice = chooseRiver();
        if (choice == null)
            protocol.passMove();
        else
            protocol.claimMove(choice.getSource(), choice.getTarget());
        System.out.println("Я походил. Не бей по лицу.");
    }

    private void printMineInfo() {
        System.out.println("-----");
        for (Map.Entry<Integer, Integer> mine: minesInfo.entrySet()){
            System.out.println("Шахта: " + mine.getKey() + ", Рек: " + mine.getValue());
        }
        System.out.println("-----");
    }


}
