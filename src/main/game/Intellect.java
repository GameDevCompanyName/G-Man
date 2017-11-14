package game;

import gamedev.protocol.Protocol;
import gamedev.protocol.data.River;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Intellect {

    private State state;
    private Protocol protocol;

    public Intellect(State state, Protocol protocol){
        this.state = state;
        this.protocol = protocol;
    }

    private River randomRiver(Map<River, RiverState> rivers){
        List<River> neutralRivers = new ArrayList<>();
        for (Map.Entry<River, RiverState> obj:rivers.entrySet()) {
            if (obj.getValue() == RiverState.Neutral){
                neutralRivers.add(obj.getKey());
            }
        }
        Random random = new Random();
        return neutralRivers.get(random.nextInt(neutralRivers.size()));
    }

    public void makeMove(){
        River choice = randomRiver(state.getRivers());
        protocol.claimMove(choice.getSource(), choice.getTarget());
    }
}
