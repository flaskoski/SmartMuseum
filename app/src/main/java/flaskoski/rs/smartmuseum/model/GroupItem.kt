package flaskoski.rs.smartmuseum.model

class GroupItem(
        var subItems: Set<Element> = HashSet()
) : Item()