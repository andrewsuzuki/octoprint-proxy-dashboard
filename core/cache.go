package core

import (
    "github.com/patrickmn/go-cache"
)

var c *cache.Cache

func init() {
    c = cache.New(cache.NoExpiration, -1)
}

func SetCached(k string, x interface{}) {
    c.SetDefault(k, x)
}

func GetCached(k string) (interface{}, bool) {
    return c.Get(k)
}
