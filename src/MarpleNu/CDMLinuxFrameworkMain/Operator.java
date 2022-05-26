package MarpleNu.CDMLinuxFrameworkMain;

public class Operator {
    int operate;
    public long timeStamp;
    Operator(int operates, long timestamp){
        operate = operates;
        timeStamp = timestamp;
    }
    @Override
    public boolean equals(Object operator){
        if (operator == this) return true;
        if (!(operator instanceof Operator)) {
            return false;
        }
        Operator o = (Operator) operator;
        return operate == o.operate && timeStamp == o.timeStamp;
    }
    @Override
    public int hashCode() {
        return (operate + "" + timeStamp).hashCode();
    }
}
