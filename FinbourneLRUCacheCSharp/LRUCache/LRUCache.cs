using System;
using System.Collections.Generic;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace LRUCache;

public sealed class LRUCache
{
    private static readonly Lazy<LRUCache> lazy =
        new Lazy<LRUCache>(() => new LRUCache());

    public static LRUCache Instance { get { return lazy.Value; } }

    /// <summary>
    /// The default capacity value should be chosen appropriately, and can be altered using the resize method
    /// </summary>
    private int _capacity = 99;
    private Dictionary<object, Node> _map = new();
    
    //LRU item
    private Node _head = null;
    
    //Next to be evicted
    private Node _tail = null;

    /// <summary>
    /// Lock for all functions accessing the cache
    /// </summary>
    private object _lock = new();

    public Dictionary<object, object> Resize(int newCapacity) {
        lock (_lock)
        {
            if (newCapacity < 0)
            {
                throw new ArgumentException("Cache size can not be negative (" + newCapacity + ")");
            }
            else
            {
                _capacity = newCapacity;
                Dictionary<object, object> removed = new();
                while (Size() > _capacity)
                {
                    Node n = removeTail();
                    removed.Add(n.Key, n.Value);
                }
                return removed;
            }
        }
    }

    private Node removeTail()
    {
        lock (_lock)
        {
            if (_tail != null)
            {
                Node toRemove = _tail;
                if (toRemove.EvictionCallback != null)
                {
                    toRemove.EvictionCallback.Invoke(toRemove.Key, toRemove.Value);
                }
                _map.Remove(toRemove.Key);
                Node prev = toRemove.Prev;
                if (prev != null)
                {
                    prev.Next = null;
                    _tail = prev;
                }
                else
                {
                    _tail = null;
                    _head = null;
                }
                return toRemove;
            }
            else
            {
                return null;
            }
        }
    }

    private void moveToTail(Node n)
    {
        lock (_lock)
        {
            if (n != _tail)
            {
                if (n.Prev != null)
                {
                    n.Prev.Next = n.Next;
                }
                if (n.Next != null)
                {
                    n.Next.Prev = n.Prev;
                    if (n.Next.Prev == null)
                    {
                        _head = n.Next;
                    }
                }
                if (_tail != null)
                {
                    _tail.Next = n;
                    n.Prev = _tail;
                }
                _tail = n;
            }
        }
    }

    private void moveToHead(Node n)
    {
        lock (_lock)
        {
            if (n != _head)
            {
                if (n.Prev != null)
                {
                    n.Prev.Next = n.Next;
                    if (n.Prev.Next == null)
                    {
                        _tail = n.Prev;
                    }
                }
                if (n.Next != null)
                {
                    n.Next.Prev = n.Prev;
                }
                if (_head != null)
                {
                    _head.Prev = n;
                    n.Next = _head;
                }
                _head = n;
            }
        }
    }

    public int Size()
    {
        lock(_lock)
        {
            return _map.Count;
        }
    }

    public bool Empty()
    {
        lock(_lock)
        {
            return Size() == 0;
        }
    }

    public bool ContainsKey(object key)
    {
        lock (_lock)
        {
            return _map.ContainsKey(key);
        }
    }

    public bool ContainsValue(object value)
    {
        lock(_lock)
        {
            return _map.Values.Contains(value);
        }
    }

    /// <summary>
    /// Getting an item means updating its position in the LRU list
    /// </summary>
    public object Get(object key)
    {
        lock (_lock)
        {
            if (_map.ContainsKey(key))
            {
                Node n = _map[key];
                moveToHead(n);
                return n.Value;
            }
            else
            {
                return null;
            }
        }
    }

    public object Put(object key, object value)
    {
        lock (_lock) 
        {
            return Put(key, value, null);
        }
    }

    public object Put(object key, object value, Action<object, object> evictionCallback)
    {
        lock (_lock)
        {
            object previousValue = Get(key);

            Node n = new Node(key, value);
            n.EvictionCallback = evictionCallback;
            moveToHead(n);
            if (Empty())
            {
                // If first item it's both head and tail
                moveToTail(n);
            }
            _map.Add(key, n);

            while (Size() > _capacity)
            {
                removeTail();
            }

            return previousValue;
        }
    }


    public object Remove(object key)
    {
        lock (_lock)
        {
            if (_map.ContainsKey(key))
            {
                Node n = _map[key];
                _map.Remove(key);
                moveToTail(n);
                removeTail();
                return n.Value;
            }
            else
            {
                return null;
            }
        }
    }

    public void PutAll(Dictionary<object, object> m)
    {
        lock(_lock)
        {
            foreach(KeyValuePair<object, object> pair in m)
            {
                Put(pair.Key, pair.Value); 
            }
        }
    }

    public override bool Equals(object? obj)
    {
        return obj is LRUCache cache &&
               _capacity == cache._capacity &&
               EqualityComparer<Dictionary<object, Node>>.Default.Equals(_map, cache._map);
    }

    public void Clear()
    {
        lock (_lock)
        {
            _map.Clear();
            _head = null;
            _tail = null;
        }
    }

    public override int GetHashCode()
    {
        return HashCode.Combine(_capacity, _map);
    }
}