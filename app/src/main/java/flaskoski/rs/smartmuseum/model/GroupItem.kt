package flaskoski.rs.smartmuseum.model
/**
 * Copyright (c) 2019 Felipe Ferreira Laskoski
 * código fonte licenciado pela MIT License - https://opensource.org/licenses/MIT
 */
class GroupItem(
        var subItems: List<String> = ArrayList()
) : Item()