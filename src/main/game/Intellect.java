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

    public Intellect(State state, Protocol protocol) {
        this.state = state;
        this.protocol = protocol;
        fillMinesInfo();
    }

    //Здесь мы инициализируем нашу таблицу с шахтами, проходясь
    //по всем рекам и считая, сколько рек находится возле каждой
    //шахты.
    private void fillMinesInfo() {
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
        return neutralRivers.get(random.nextInt(neutralRivers.size()));
    }

    //Метод с основной логикой выбора реки
    private River chooseRiver(Map<River, RiverState> rivers) {

        River choose = null;

        if (!minesInfo.isEmpty()) {
            choose = claimFromMineInfo();
        } else {
            choose = randomRiver();
        }

        return choose;
    }

    //Выбор реки исходя из наших знаний о шахтах
    private River claimFromMineInfo() {
        int mineId = minesInfo.entrySet().iterator().next().getKey();
        minesInfo.remove(mineId);
        return findRiver(mineId);
    }

    //Найти реку по ID
    private River findRiver(int mineId) {
        River result = null;
        for (Map.Entry<River, RiverState> river : state.getRivers().entrySet()) {
            if (river.getValue() == RiverState.Neutral) {
                if (river.getKey().component1() == mineId || river.getKey().component2() == mineId)
                    result = river.getKey();
                break;
            }
        }
        return result;
    }

    //Делает ход
    public void makeMove() {
        //River choice = randomRiver(state.getRivers());
        River choice = chooseRiver(state.getRivers());
        if (choice == null)
            protocol.passMove();
        else
            protocol.claimMove(choice.getSource(), choice.getTarget());
        System.out.println("Я походил. Не бей по лицу.");
    }


}
