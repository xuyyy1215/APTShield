package MarpleNu.CDMDataParser;
import java.util.Objects;
public class EventLeft extends Object{
    private String Event;
    private String SubjectID;
    private String ObjectID;
    private String ObjectID2;
    private String Time;

    public EventLeft() {
    }

    public EventLeft(String Event, String SubjectID,String ObjectID,String ObjectID2,String Time) {
        this.Event = Event;
        this.SubjectID = SubjectID;
        this.ObjectID = ObjectID;
        this.ObjectID2 = ObjectID2;
        this.Time = Time;
    }

    public void setEvent(String Event) {
        this.Event = Event;
    }

    public String getEvent() {
        return Event;
    }

    public void setSubjectID(String SubjectID) {
        this.SubjectID = SubjectID;
    }

    public String getSubjectID() {
        return SubjectID;
    }

    public void setObjectID(String ObjectID) {
        this.ObjectID = ObjectID;
    }

    public String getObjectID() {
        return ObjectID;
    }

    public void setObjectID2(String ObjectID2) {
        this.ObjectID2 = ObjectID2;
    }

    public String getObjectID2() {
        return ObjectID2;
    }

    public void setTime(String Time) {
        this.Time = Time;
    }

    public String getTime() {
        return Time;
    }


    //@Override
    /*public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Person person = (Person) o;
        return age == person.age &&
                Objects.equals(name, person.name);
    }*/

    @Override
    public int hashCode() {

        return Objects.hash(Event, SubjectID,ObjectID,Time);
    }
    public int objecthashCode() {

        return Objects.hash(ObjectID,ObjectID2);
    }

    @Override
    public String toString() {
        return "EventLeft{" +
                "Event='" + Event + '\'' +
                ", SubjectID='" + SubjectID +'\'' +
                ", ObjectID='" + ObjectID + '\'' +
                ", Time=" + Time +
                '}';
    }
}
