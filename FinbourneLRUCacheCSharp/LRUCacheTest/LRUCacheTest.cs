using LRUCache;

namespace LRUCacheTest
{
    [TestClass]
    public class LRUCacheTest
    {

        LRUCache.LRUCache cache = LRUCache.LRUCache.Instance;

        [TestInitialize]
        public void BeforeEach()
        {
            cache.Clear();
            cache.Resize(99);
        }

        [TestMethod]
        public void IsSingleton()
        {
            Assert.AreSame(cache, LRUCache.LRUCache.Instance);
        }

        [TestMethod]
        public void BasicPutAndGet()
        {
            cache.Put("x", "xx");
            cache.Put(true, false);
            Assert.AreEqual(cache.Size(), 2);
            Assert.AreEqual(cache.Get("x"), "xx");
            Assert.AreEqual(cache.Get(true), false);
        }

        [TestMethod]        
        public void BasicPutAndRemove()
        {
            cache.Put("x", "xx");
            cache.Put(true, "false");
            Assert.AreEqual(cache.Size(), 2);
            Assert.AreEqual(cache.Remove(true), "false");
            Assert.IsNull(cache.Remove(99));
            Assert.AreEqual(cache.Size(), 1);
            Assert.IsNull(cache.Get(true));
        }

        [TestMethod]
        public void MaxCapacity()
        {
            cache.Resize(1);
            cache.Put(1, 11);
            cache.Put(2, 22);
            Assert.AreEqual(cache.Size(), 1);
            Assert.IsNull(cache.Get(1));
            Assert.AreEqual(cache.Get(2), 22);
        }


        [TestMethod]
        public void GetShouldRefreshLRU() 
        {
            cache.Put(1, 11);
            cache.Put(2, 22);
            cache.Get(1);
            cache.Resize(1);
            Assert.AreEqual(cache.Size(), 1);
            Assert.IsNull(cache.Get(2));
            Assert.AreEqual(cache.Get(1), 11);
        }

        [TestMethod]
        public void IllegalCapacity()
        {
            Assert.ThrowsException<ArgumentException>(() => { cache.Resize(-1); });
        }

        [TestMethod]
        public void CallBackOnEviction()
        {
            int x = 0;
            cache.Put(1, 11, (key, value) =>
            {
                x = (int)value;
            });
            cache.Remove(1);
            Assert.AreEqual(x, 11);
        }

        [TestMethod]
        public void ResizeReturnsListOfEvicted()
        {
            cache.Put(1, 11);
            cache.Put(2, 22);
            cache.Put(3, 33);
            Dictionary<object, object> expected = new();
            expected.Add(1, 11);
            expected.Add(2, 22);
            Assert.IsTrue(expected.SequenceEqual(cache.Resize(1)));
        }
    }
}